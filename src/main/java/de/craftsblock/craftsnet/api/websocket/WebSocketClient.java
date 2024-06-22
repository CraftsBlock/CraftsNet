package de.craftsblock.craftsnet.api.websocket;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftscore.annotations.Experimental;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.requirements.Requirement;
import de.craftsblock.craftsnet.api.transformers.TransformerPerformer;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtension;
import de.craftsblock.craftsnet.events.sockets.*;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * The WebSocketClient class represents a WebSocket client that connects to the WebSocketServer.
 * <p>
 * When a WebSocket client connects, the server creates a new instance of this class to manage the client's connection.
 * The client sends a handshake request, and if the server validates it, the client is assigned to a specific endpoint
 * based on the requested path. The client continuously listens for incoming messages and invokes the appropriate
 * endpoint's method to process the received data.
 * <p>
 * The class also handles the disconnection of the client, sending messages, and reading headers and messages from the socket.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 3.0.0
 * @see WebSocketServer
 * @since 2.1.1-SNAPSHOT
 */
public class WebSocketClient implements Runnable, RequireAble {

    private final WebSocketServer server;
    private final Socket socket;
    private final WebSocketStorage storage;
    private final List<WebSocketExtension> extensions;

    private SocketExchange exchange;
    private Headers headers;
    private String ip;
    private String path;
    private String domain;
    private List<RouteRegistry.EndpointMapping> mappings;

    private BufferedReader reader;
    private OutputStream writer;

    private final CraftsNet craftsNet;
    private final Logger logger;

    private boolean active = false;
    private boolean connected = false;

    private boolean shouldFragment;
    private int fragmentSize;

    private int closeCode = -1;
    private String closeReason = null;
    private boolean closeByServer = false;

    /**
     * Creates a new WebSocketClient with the provided socket and server.
     *
     * @param craftsNet The CraftsNet instance which instantiates this
     * @param socket    The Socket used for communication with the client.
     * @param server    The WebSocketServer to which this client belongs.
     */
    public WebSocketClient(CraftsNet craftsNet, Socket socket, WebSocketServer server) {
        this.socket = socket;
        this.server = server;
        this.storage = new WebSocketStorage();
        this.extensions = new ArrayList<>();

        this.shouldFragment = server.shouldFragment();
        this.fragmentSize = -1;
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.logger();
    }

    /**
     * Runs the WebSocket client to handle incoming messages and manage connections.
     * This method reads the client's headers, sends a handshake to establish the WebSocket connection,
     * and then processes incoming messages from the client using a registered endpoint.
     */
    @Override
    public void run() {
        // Ensure that the client is not already active
        if (this.active)
            throw new IllegalStateException("This websocket client is already running!");

        this.exchange = new SocketExchange(server, this); // Create a SocketExchange object to handle communication with the server
        this.active = true;
        try {
            // Setup input and output streams for communication with the client
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = socket.getOutputStream();
            headers = readHeaders(); // Read and store the client's headers for later use

            // Abort if the path was not found on the request
            if (path == null) {
                logger.warning("The path could not be loaded. (Maybe an unsupported request method?)");
                disconnect();
                return;
            }

            // Determine the host domain from headers
            AtomicReference<String> host = new AtomicReference<>();
            if (headers.containsKey("X-Forwarded-Host")) host.set(headers.getFirst("X-forwarded-Host").split(":")[0]);
            else host.set(headers.getFirst("Host").split(":")[0]);
            this.domain = host.get();

            // Determine the client's IP address from headers, taking into account any proxy headers
            ip = socket.getInetAddress().getHostAddress();
            if (getHeader("X-forwarded-for") != null)
                ip = Objects.requireNonNull(getHeader("X-forwarded-for")).split(", ")[0];
            if (getHeader("Cf-connecting-ip") != null)
                ip = getHeader("Cf-connecting-ip");

            // Loadup extensions
            for (String protocol : getHeader("Sec-websocket-extensions").split(";\\s"))
                if (craftsNet.webSocketExtensionRegistry().hasExtension(protocol))
                    this.extensions.add(craftsNet.webSocketExtensionRegistry().getExtensionByName(protocol));

            // Send a WebSocket handshake to establish the connection
            sendHandshake();
            this.connected = true;

            // Reverse the extension list as it is required to process the more important one's at the end
            Collections.reverse(extensions);

            sendMessage("Hello du sack");

            // Check if the requested path has a corresponding endpoint registered in the server
            this.mappings = getEndpoint();
            if (mappings == null || mappings.isEmpty()) {
                // If the requested path has no corresponding endpoint, send an error message
                logger.debug(ip + " connected to " + path + " \u001b[38;5;9m[NOT FOUND]");
                closeInternally(ClosureCode.BAD_GATEWAY, Json.empty().set("error", "Path do not match any API endpoint!").asString(), true);
                return;
            }

            Pattern validator = mappings.get(0).validator();
            Matcher matcher = validator.matcher(path);
            if (!matcher.matches()) {
                sendMessage(Json.empty()
                        .set("error", "There was an unexpected error while matching!")
                        .asString());
                return;
            }

            // Add this WebSocket client to the server's collection and mark it as connected
            server.add(path, this);

            // Trigger the ClientConnectEvent to handle the client connection
            ClientConnectEvent event = new ClientConnectEvent(exchange, mappings);
            craftsNet.listenerRegistry().call(event);

            // If the event is cancelled, disconnect the client
            if (event.isCancelled()) {
                if (event.getReason() != null)
                    sendMessage(event.getReason());
                disconnect();
                logger.debug(ip + " connected to " + path + " \u001b[38;5;9m[" + event.getReason() + "]");
                return;
            }

            // If the event is not cancelled, process incoming messages from the client
            logger.info(ip + " connected to " + path);

            // Create a transformer performer which handles all transformers
            TransformerPerformer transformerPerformer = new TransformerPerformer(this.craftsNet, validator, 2, e -> {
                sendMessage(Json.empty().set("error", "Could not process transformer: " + e.getMessage()).asString());
                disconnect();
            });

            // Prepare mappings
            ConcurrentHashMap<ProcessPriority.Priority, List<RouteRegistry.EndpointMapping>> mappedMappings = new ConcurrentHashMap<>();
            for (RouteRegistry.EndpointMapping mapping : mappings)
                mappedMappings.computeIfAbsent(mapping.priority(), m -> new ArrayList<>()).add(mapping);

            Frame frame;
            while (!Thread.currentThread().isInterrupted() && isConnected() && (frame = readMessage()) != null) {
                if (!this.connected || !this.socket.isConnected()) break;
                // Process incoming messages from the client
                byte[] data = frame.getData();

                if (frame.getOpcode().equals(Opcode.CLOSE)) {
                    if (data == null || data.length <= 2) break;
                    closeCode = (data[0] & 0xFF) << 8 | (data[1] & 0xFF);
                    closeReason = new String(Arrays.copyOfRange(data, 2, data.length));
                    closeInternally(ClosureCode.NORMAL, "Acknowledged close", false);
                    break;
                }

                if (frame.getOpcode().equals(Opcode.PING)) {
                    craftsNet.listenerRegistry().call(new ReceivedPingMessageEvent(exchange, data));
                    continue;
                }

                if (frame.getOpcode().equals(Opcode.PONG)) {
                    craftsNet.listenerRegistry().call(new ReceivedPongMessageEvent(exchange, data));
                    continue;
                }

                if (frame.getOpcode().equals(Opcode.TEXT) &&
                        IntStream.range(0, data.length).map(i -> data[i]).anyMatch(tmp -> tmp < 0)) {
                    closeInternally(ClosureCode.UNSUPPORTED, "Send negativ byte values while the control byte is set to utf8!", true);
                    break;
                }

                // Fire an incoming socket message event and continue if it was cancelled
                IncomingSocketMessageEvent event2 = new IncomingSocketMessageEvent(exchange, data);
                craftsNet.listenerRegistry().call(event2);
                if (event2.isCancelled())
                    continue;

                // Extract and pass the message parameters to the endpoint handler
                Object[] args = new Object[matcher.groupCount() + 1];
                args[0] = exchange;
                args[1] = data;
                for (int i = 2; i <= matcher.groupCount(); i++) args[i] = matcher.group(i);

                // Loop through all priorities
                for (ProcessPriority.Priority priority : ProcessPriority.Priority.values()) {
                    if (!mappedMappings.containsKey(priority)) continue;

                    mappingLoop:
                    for (RouteRegistry.EndpointMapping mapping : mappedMappings.get(priority)) {
                        if (!(mapping.handler() instanceof SocketHandler handler)) continue;
                        Method method = mapping.method();

                        // Process the requirements
                        if (craftsNet.routeRegistry().getRequirements().containsKey(WebSocketServer.class))
                            for (Requirement requirement : craftsNet.routeRegistry().getRequirements().get(WebSocketServer.class))
                                try {
                                    Method m = requirement.getClass().getDeclaredMethod("applies", Frame.class, RouteRegistry.EndpointMapping.class);
                                    if (!((Boolean) m.invoke(requirement, frame, mapping))) continue mappingLoop;
                                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                                }

                        // Perform all transformers and continue if passingArgs is null
                        Object[] passingArgs = transformerPerformer.perform(method, args);
                        if (passingArgs == null)
                            continue;

                        // Check if the second parameter is a string and converts the message data if so
                        if (method.getParameterCount() >= 2 && method.getParameterTypes()[1].equals(String.class))
                            passingArgs[1] = new String(data, StandardCharsets.UTF_8);

                        // Check if the second parameter is a frame and overrides the passing args accordingly
                        if (method.getParameterCount() >= 2 && method.getParameterTypes()[1].equals(Frame.class))
                            passingArgs[1] = frame.clone();

                        // Invoke the handler method
                        method.invoke(handler, passingArgs);
                    }
                }
            }

            // Clear up transformer cache to free up memory
            transformerPerformer.clearCache();
        } catch (SocketException ignored) {
        } catch (Throwable t) {
            createErrorLog(t);
        } finally {
            disconnect();
        }
    }

    /**
     * Creates an error log for a specific throwable
     *
     * @param t the throwable which has been thrown
     */
    private void createErrorLog(Throwable t) {
        if (craftsNet.fileLogger() != null) {
            long errorID = craftsNet.fileLogger().createErrorLog(this.craftsNet, t, "ws", path);
            logger.error(t, "Error: " + errorID);
            sendMessage(Json.empty()
                    .set("error.message", "An unexpected exception happened whilst processing your message!")
                    .set("error.identifier", errorID));
        } else logger.error(t);
    }

    /**
     * Reads the HTTP headers from the client's request.
     *
     * @return A List of Strings containing the headers from the client's request.
     * @throws IOException If an I/O error occurs while reading the headers.
     */
    private Headers readHeaders() throws IOException {
        Headers headers = new Headers();
        String line;

        headerReader:
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (path == null)
                for (String method : HttpMethod.ALL.getMethods())
                    if (line.startsWith(method)) {
                        path = line.split(" ")[1];
                        continue headerReader;
                    }

            int colonIndex = line.indexOf(':');
            if (colonIndex <= 0) continue;

            String key = line.substring(0, colonIndex);
            String value = line.substring(colonIndex + 1).trim();

            headers.add(key, value);
        }

        return headers;
    }

    /**
     * Retrieves the registered endpoint corresponding to the given header.
     *
     * @return The corresponding list of SocketMappings if found, or null if not found.
     */
    @Nullable
    private List<RouteRegistry.EndpointMapping> getEndpoint() {
        return craftsNet.routeRegistry().getSocket(this);
    }

    /**
     * Sends a WebSocket handshake to the client to establish the connection.
     * The handshake includes the required headers for a WebSocket upgrade.
     */
    private void sendHandshake() {
        try {
            String concatenated = getHeader("Sec-WebSocket-Key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(concatenated.getBytes(StandardCharsets.UTF_8));

            String extensions = String.join(",", this.extensions.parallelStream().map(WebSocketExtension::getProtocolName).toList());
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + Base64.getEncoder().encodeToString(hash) + "\r\n"
                    + (!extensions.isEmpty() ? "Sec-websocket-extensions: " + extensions + "\r\n" : "")
                    + "\r\n";
            writer.write(response.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error(e);
        }
    }

    /**
     * Reads a WebSocket message from the client.
     *
     * @return The message as a message, or null if the client has disconnected.
     * @throws IOException If an I/O error occurs while reading the message.
     */
    private Frame readMessage() throws IOException {
        if (!isConnected()) return null;

        // Read the input stream from the socket.
        InputStream inputStream = socket.getInputStream();
        AtomicReference<Frame> frame = new AtomicReference<>();

        while (frame.get() == null || !frame.get().isFinalFrame()) {
            try {
                Frame read = Frame.read(inputStream);
                if (frame.get() == null) {
                    frame.set(read);
                    continue;
                }

                frame.get().appendFrame(read);
            } catch (SocketException e) {
                throw e;
            } catch (NullPointerException | IOException e) {
                createErrorLog(e);
            }
        }

        Frame read = frame.get();
        for (WebSocketExtension extension : this.extensions)
            read = extension.decode(read);

        return read;
    }

    /**
     * Returns the requested path from the client's headers.
     *
     * @return The requested path as a String.
     */
    public String getPath() {
        return path;
    }

    /**
     * Retrieves the value of the given header key from the client's headers.
     *
     * @param key The header key for which the value is requested.
     * @return The value of the header if found, or null if the header is not present.
     */
    public String getHeader(String key) {
        return headers.getFirst(key);
    }

    /**
     * Returns the IP address of the connected client.
     *
     * @return The IP address as a String.
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns the domain used by the connected client.
     *
     * @return The domain as a String.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Returns a List containing all the headers received from the client.
     *
     * @return A List of Strings representing the headers from the client.
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Returns the storage of this websocket client for storing specific data.
     *
     * @return The storage of this websocket client instance.
     */
    public WebSocketStorage getStorage() {
        return storage;
    }

    /**
     * Returns whether fragmentation is enabled or not.
     *
     * @return true if fragmentation is enabled, false otherwise
     */
    @Experimental
    public boolean shouldFragment() {
        return server.shouldFragment() && shouldFragment || shouldFragment;
    }

    /**
     * Enable or disable fragmentation of messages send by the server.
     *
     * @param shouldFragment true if fragmentation should be enabled, false otherwise.
     */
    @Experimental
    public void setFragmentationEnabled(boolean shouldFragment) {
        this.shouldFragment = shouldFragment;
    }

    /**
     * Returns the size which should every fragment of a frame should have.
     *
     * @return The max size of each frame.
     */
    @Experimental
    public int getFragmentSize() {
        if (fragmentSize <= 0) return server.getFragmentSize();
        return fragmentSize;
    }

    /**
     * Sets the maximum size of each fragment of a frame.
     *
     * @param fragmentSize The max size of the fragments.
     */
    @Experimental
    public void setFragmentSize(int fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    /**
     * Sends a message to the connected WebSocket client.
     *
     * @param data The message to be sent, as it's json representation.
     */
    public void sendMessage(Json data) {
        sendMessage(data.toString());
    }

    /**
     * Sends a message to the connected WebSocket client.
     *
     * @param data The message to be sent, as it's string representation.
     */
    public void sendMessage(String data) {
        sendMessage(data.getBytes(StandardCharsets.UTF_8), Opcode.TEXT);
    }

    /**
     * Sends a message to the connected WebSocket client.
     *
     * @param data The message to be sent, as a byte array.
     */
    public void sendMessage(byte[] data) {
        sendMessage(data, Opcode.BINARY);
    }

    /**
     * Sends a ping to the connected WebSocket client.
     */
    public void sendPing() {
        sendPing((byte[]) null);
    }

    /**
     * Sends a ping with the provided payload to the connected WebSocket client.
     *
     * @param message The payload as a string which should be appended
     */
    public void sendPing(String message) {
        sendPing(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a ping with the provided payload to the connected WebSocket client.
     *
     * @param message The payload as a byte array which should be appended
     */
    public void sendPing(byte[] message) {
        sendMessage(message, Opcode.PING);
    }

    /**
     * Sends a pong to the connected WebSocket client.
     */
    public void sendPong() {
        sendPong((byte[]) null);
    }

    /**
     * Sends a pong with the provided payload to the connected WebSocket client.
     *
     * @param message The payload as a string which should be appended
     */
    public void sendPong(String message) {
        sendPong(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a pong with the provided payload to the connected WebSocket client.
     *
     * @param message The payload as a byte array which should be appended
     */
    public void sendPong(byte[] message) {
        sendMessage(message, Opcode.PONG);
    }

    /**
     * Disconnects the client gracefully without providing any information about the reason.
     */
    public void close() {
        sendMessage(null, Opcode.CLOSE);
    }

    /**
     * Disconnects the client gracefully with a specified reason.
     *
     * @param reason The reason why the client has been closed.
     * @throws IllegalStateException If the code used to close the connection is only for internal use.
     */
    public void close(String reason) {
        close(ClosureCode.NORMAL, reason);
    }

    /**
     * Disconnects the client gracefully with a specified pre-defined code and reason.
     *
     * @param code   The pre-defined code that is responsible for closing the socket.
     * @param reason The reason why the client has been closed.
     * @throws IllegalStateException If the code used to close the connection is only for internal use.
     */
    public void close(ClosureCode code, String reason) {
        close(code.intValue(), reason);
    }

    /**
     * Disconnects the client gracefully with a specified code and reason.
     *
     * @param code   The close code.
     * @param reason The reason why the client has been closed.
     * @throws IllegalStateException If the code used to close the connection is only for internal use.
     */
    public void close(int code, String reason) {
        if (ClosureCode.RAW_INTERNAL_CODES.contains(code)) {
            closeInternally(ClosureCode.SERVER_ERROR, "Used close code " + code, true);
            throw new IllegalStateException("The close code " + code + " was used, but is not meant to use!");
        }

        closeInternally(code, reason, true);
    }

    /**
     * Closes the client internally with a pre-defined close code and a reason.
     *
     * @param code   The pre-defined close code.
     * @param reason The reason why the client has been closed.
     */
    private void closeInternally(ClosureCode code, String reason, boolean closeByServer) {
        closeInternally(code.intValue(), reason, closeByServer);
    }

    /**
     * Closes the client internally with a close code and a reason.
     *
     * @param code   The close code.
     * @param reason The reason why the client has been closed.
     */
    private synchronized void closeInternally(int code, String reason, boolean closeByServer) {
        byte[] message = reason.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[2 + message.length];

        // Convert the code to an unsigned short and write it as the first two bytes
        data[0] = (byte) (code >> 8);
        data[1] = (byte) code;

        // Copy the message into the data array
        System.arraycopy(message, 0, data, 2, message.length);

        // Fire the message
        sendMessage(data, Opcode.CLOSE);

        this.closeCode = code;
        this.closeReason = reason;
        this.closeByServer = closeByServer;
        this.connected = false;
    }

    /**
     * Sends a message with a specific control byte to the connected WebSocket client.
     *
     * @param data   The message to be sent, as a byte array.
     * @param opcode The byte used to control the message flow.
     */
    private synchronized void sendMessage(byte[] data, Opcode opcode) {
        if (!isConnected())
            throw new IllegalStateException("The websocket connection has already been closed!");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (data == null || data.length == 0) {
                outputStream.write(0x80 | opcode.byteValue());
                outputStream.write(0x00);
                writer.write(outputStream.toByteArray());
                return;
            }

            OutgoingSocketMessageEvent event = new OutgoingSocketMessageEvent(new SocketExchange(server, this), opcode, data);
            if (!opcode.equals(Opcode.CLOSE) && !opcode.equals(Opcode.CONTINUATION)) {
                craftsNet.listenerRegistry().call(event);
                if (event.isCancelled())
                    return;
            }

            Frame frame = new Frame(true, false, false, false, event.getOpcode(), event.getData());

            if (shouldFragment())
                for (Frame send : frame.fragmentFrame(getFragmentSize())) {
                    for (WebSocketExtension extension : this.extensions)
                        send = extension.encode(send);

                    outputStream.reset();
                    send.write(outputStream);
                    writer.write(outputStream.toByteArray());
                }
            else {
                for (WebSocketExtension extension : this.extensions)
                    frame = extension.encode(frame);

                outputStream.reset();
                frame.write(outputStream);
                writer.write(outputStream.toByteArray());
            }
        } catch (SocketException ignored) {
        } catch (IOException e) {
            logger.error(e);
            disconnect();
        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.error(e);
        }
    }

    /**
     * Splits a byte array into smaller arrays based on a specified maximum size.
     *
     * @param input   The byte array to be split.
     * @param maxSize The maximum size of each chunk.
     * @return A list of byte arrays, each with a size up to the specified maximum size.
     */
    private List<byte[]> splitByteArray(byte[] input, int maxSize) {
        List<byte[]> result = new ArrayList<>();
        int inputLength = input.length;

        for (int start = 0; start < inputLength; start += maxSize) {
            int end = Math.min(inputLength, start + maxSize);
            byte[] chunk = new byte[end - start];
            System.arraycopy(input, start, chunk, 0, end - start);
            result.add(chunk);
        }

        return result;
    }

    /**
     * Returns whether the websocket runnable has been started.
     *
     * @return True if the websocket runnable was started, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns whether the web socket is still connected or the connection has been closed / failed.
     *
     * @return True if the websocket is connected, false otherwise or if the connection failed
     */
    public boolean isConnected() {
        return this.connected && this.socket.isConnected();
    }

    /**
     * Disconnects the WebSocket client and performs necessary cleanup operations.
     * This method triggers the ClientDisconnectEvent before closing the socket and removing the client from the server.
     *
     * @return The Thread the client is running from
     */
    protected synchronized Thread disconnect() {
        if (this.connected || socket.isConnected())
            try {
                if (reader == null || writer == null) return Thread.currentThread();
                craftsNet.listenerRegistry().call(new ClientDisconnectEvent(new SocketExchange(server, this), closeCode, closeReason, closeByServer, mappings));

                if (reader != null) reader.close();
                reader = null;
                if (writer != null) writer.close();
                writer = null;

                if (socket != null) socket.close();

                if (!closeByServer && this.connected) logger.warning(ip + " disconnected abnormal: The underlying tcp connection has been killed!");
                else if (!closeByServer && closeCode != -1 && closeCode != ClosureCode.NORMAL.intValue()) {
                    ClosureCode code = ClosureCode.fromInt(closeCode);
                    logger.warning(
                            ip + " disconnected abnormal " +
                                    "(Code: " + (code != null ? code : closeCode) + ")" +
                                    (closeReason != null && !closeReason.isEmpty() ? ": " + closeReason : "")
                    );
                } else
                    logger.debug(ip + " disconnected");

                headers = null;
                mappings = null;
                storage.clear();
                extensions.clear();
                server.remove(this);
                this.connected = false;
            } catch (InvocationTargetException | IllegalAccessException | IOException e) {
                logger.error(e);
            }
        return Thread.currentThread();
    }

}

