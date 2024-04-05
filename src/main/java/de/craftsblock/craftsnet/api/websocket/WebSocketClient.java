package de.craftsblock.craftsnet.api.websocket;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.events.sockets.ClientConnectEvent;
import de.craftsblock.craftsnet.events.sockets.ClientDisconnectEvent;
import de.craftsblock.craftsnet.events.sockets.IncomingSocketMessageEvent;
import de.craftsblock.craftsnet.events.sockets.OutgoingSocketMessageEvent;
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
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
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
 * @version 1.7
 * @see WebSocketServer
 * @since 2.1.1
 */
public class WebSocketClient implements Runnable {

    private final WebSocketServer server;
    private final Socket socket;
    private SocketExchange exchange;
    private Headers headers;
    private String ip;
    private RouteRegistry.SocketMapping mapping;
    private String path;

    private BufferedReader reader;
    private OutputStream writer;

    private final Logger logger = CraftsNet.logger();

    private boolean active = false;
    private boolean connected = false;

    /**
     * Creates a new WebSocketClient with the provided socket and server.
     *
     * @param socket The Socket used for communication with the client.
     * @param server The WebSocketServer to which this client belongs.
     */
    public WebSocketClient(Socket socket, WebSocketServer server) {
        this.socket = socket;
        this.server = server;
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

            // Determine the host domain from headers
            AtomicReference<String> host = new AtomicReference<>(getHeader("Host"));
            if (host.get() == null || host.get().isBlank()) host.set(null);
            else host.set(host.get().split(":")[0]);

            // Determine the client's IP address from headers, taking into account any proxy headers
            ip = socket.getInetAddress().getHostAddress();
            if (getHeader("X-forwarded-for") != null)
                ip = Objects.requireNonNull(getHeader("X-forwarded-for")).split(", ")[0];
            if (getHeader("Cf-connecting-ip") != null)
                ip = getHeader("Cf-connecting-ip");

            // Send a WebSocket handshake to establish the connection
            sendHandshake();

            // Check if the requested path has a corresponding endpoint registered in the server
            mapping = getEndpoint(path, host.get());
            if (mapping != null) {
                Matcher matcher = mapping.validator().matcher(path);
                if (!matcher.matches()) {
                    sendMessage(JsonParser.parse("{}")
                            .set("error", "There was an unexpected error while matching!")
                            .asString());
                    return;
                }

                // Trigger the ClientConnectEvent to handle the client connection
                ClientConnectEvent event = new ClientConnectEvent(exchange, mapping);
                CraftsNet.listenerRegistry().call(event);

                // If the event is cancelled, disconnect the client
                if (event.isCancelled()) {
                    if (event.getReason() != null)
                        sendMessage(event.getReason());
                    disconnect();
                    logger.debug(ip + " connected to " + path + " \u001b[38;5;9m[ABORTED]");
                    return;
                }

                // If the event is not cancelled, process incoming messages from the client
                logger.info(ip + " connected to " + path);

                // Add this WebSocket client to the server's collection
                server.add(path, this);

                connected = true;
                String message;
                while (!Thread.currentThread().isInterrupted() && (message = readMessage()) != null) {
                    // Process incoming messages from the client
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    if (IntStream.range(0, data.length).map(i -> data[i]).anyMatch(tmp -> tmp < 0)) break;
                    if (message.isEmpty() || message.isBlank()) break;

                    // Fire an incoming socket message event and continue if it was cancelled
                    IncomingSocketMessageEvent event2 = new IncomingSocketMessageEvent(exchange, message);
                    CraftsNet.listenerRegistry().call(event2);
                    if (event2.isCancelled())
                        continue;

                    // Extract and pass the message parameters to the endpoint handler
                    Object[] args = new Object[matcher.groupCount() + 1];
                    args[0] = exchange;
                    args[1] = message;
                    for (int i = 2; i <= matcher.groupCount(); i++) args[i] = matcher.group(i);

                    // Invoke the registered handler method for the incoming message
                    for (Method method : mapping.receiver()) {
                        // Todo: Load and use the @Transformers of the Method
                        //       Important: Cache results to faster reply on the next method!
                        method.invoke(mapping.handler(), args);
                    }
                }
            } else {
                // If the requested path has no corresponding endpoint, send an error message
                logger.debug(ip + " connected to " + path + " \u001b[38;5;9m[NOT FOUND]");
                sendMessage(JsonParser.parse("{}").set("error", "Path do not match any API endpoint!").asString());
            }
        } catch (SocketException ignored) {
        } catch (IOException | InvocationTargetException | IllegalAccessException e) {
            logger.error(e);
        } finally {
            disconnect();
        }
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

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (path == null && line.startsWith("GET")) {
                path = line.split(" ")[1];
                continue;
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
     * @param path The path to find the endpoint for.
     * @param host The domain used to connect the endpoint.
     * @return The corresponding SocketMapping if found, or null if not found.
     */
    @Nullable
    private RouteRegistry.SocketMapping getEndpoint(String path, String host) {
        return CraftsNet.routeRegistry().getSocket(path, host);
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

            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + Base64.getEncoder().encodeToString(hash) + "\r\n"
                    + "\r\n";
            writer.write(response.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error(e);
        }
    }

    /**
     * Reads a WebSocket message from the client.
     *
     * @return The message as a String, or null if the client has disconnected.
     * @throws IOException If an I/O error occurs while reading the message.
     */
    private String readMessage() throws IOException {
        if (socket.isClosed()) return null;

        // Read the input stream from the socket.
        InputStream inputStream = socket.getInputStream();

        // Create a 10-byte buffer to store the received message.
        byte[] frame = new byte[10];
        // Read the first two bytes of the frame (header) from the input stream.
        inputStream.read(frame, 0, 2);

        // Extract the payload length from the second byte of the header.
        byte payloadLength = (byte) (frame[1] & 0x7F);
        if (payloadLength == 126)
            // If the payload length is 126, read an additional 2 bytes for the actual length.
            inputStream.read(frame, 2, 2);
        else if (payloadLength == 127)
            // If the payload length is 127, read an additional 8 bytes for the actual length.
            inputStream.read(frame, 2, 8);

        // Calculate the actual value of the payload length based on the read bytes
        long payloadLengthValue = getPayloadLengthValue(frame, payloadLength);

        // Read the 4 bytes of the masking key (masks) from the input stream.
        byte[] masks = new byte[4];
        inputStream.read(masks);

        // Create a ByteArrayOutputStream to store the payload data.
        ByteArrayOutputStream payloadBuilder = new ByteArrayOutputStream();

        // Initialize variables to track the number of bytes read and the remaining bytes to read.
        long bytesRead = 0;
        long bytesToRead = payloadLengthValue;

        // Create a 4,096-byte chunk to read the payload data in pieces.
        byte[] chunk = new byte[4096];
        int chunkSize;

        // Read the payload data in chunks from the input stream and remove the masking.
        while (bytesToRead > 0 && (chunkSize = inputStream.read(chunk, 0, (int) Math.min(chunk.length, bytesToRead))) != -1) {
            for (int i = 0; i < chunkSize; i++)
                chunk[i] ^= masks[(int) ((bytesRead + i) % 4)]; // Remove the masking from each byte.
            payloadBuilder.write(chunk, 0, chunkSize); // Append the unmasked data to the payloadBuilder.

            // Update the tracked variables.
            bytesRead += chunkSize;
            bytesToRead -= chunkSize;
        }

        // Convert the read payload data to a string using UTF-8 encoding and return it.
        return payloadBuilder.toString(StandardCharsets.UTF_8);
    }

    /**
     * Extracts the payload length value from the WebSocket frame header.
     *
     * @param frame         The WebSocket frame header.
     * @param payloadLength The payload length value from the frame.
     * @return The actual payload length value.
     */
    private long getPayloadLengthValue(byte[] frame, byte payloadLength) {
        if (payloadLength == 126)
            // If the payload length is 126, combine the 3rd and 4th bytes to get the actual length.
            return ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
        else if (payloadLength == 127)
            // If the payload length is 127, combine the 3rd to 10th bytes to get the actual length.
            return ((frame[2] & 0xFFL) << 56)
                    | ((frame[3] & 0xFFL) << 48)
                    | ((frame[4] & 0xFFL) << 40)
                    | ((frame[5] & 0xFFL) << 32)
                    | ((frame[6] & 0xFFL) << 24)
                    | ((frame[7] & 0xFFL) << 16)
                    | ((frame[8] & 0xFFL) << 8)
                    | (frame[9] & 0xFFL);
        else
            // For payload lengths less than 126, the payloadLength itself represents the actual length.
            return payloadLength;
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
     * Returns a List containing all the headers received from the client.
     *
     * @return A List of Strings representing the headers from the client.
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Sends a message to the connected WebSocket client.
     *
     * @param data The message to be sent, as a String.
     */
    public void sendMessage(String data) {
        try {
            OutgoingSocketMessageEvent event = new OutgoingSocketMessageEvent(new SocketExchange(server, this), data);
            CraftsNet.listenerRegistry().call(event);
            if (event.isCancelled())
                return;

            data = event.getData();
            byte[] rawData = data.getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write((byte) 0x81);

            int length = rawData.length;
            if (length <= 125)
                outputStream.write((byte) length);
            else if (length <= 65535) {
                outputStream.write((byte) 126);
                outputStream.write((byte) (length >> 8));
                outputStream.write((byte) length);
            } else {
                outputStream.write((byte) 127);
                for (int i = 7; i >= 0; i--)
                    outputStream.write((byte) (length >> (8 * i)));
            }

            outputStream.write(rawData);
            writer.write(outputStream.toByteArray());
        } catch (SocketException ignored) {
        } catch (IOException e) {
            logger.error(e);
            disconnect();
        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.error(e);
        }
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
        return connected;
    }

    /**
     * Disconnects the WebSocket client and performs necessary cleanup operations.
     * This method triggers the ClientDisconnectEvent before closing the socket and removing the client from the server.
     *
     * @return The Thread the client is running from
     */
    public Thread disconnect() {
        try {
            if (reader == null || writer == null) return Thread.currentThread();
            CraftsNet.listenerRegistry().call(new ClientDisconnectEvent(new SocketExchange(server, this), mapping));
            if (reader != null) reader.close();
            reader = null;
            if (writer != null) writer.close();
            writer = null;
            if (socket != null) socket.close();
            logger.debug(ip + " disconnected");
            ip = null;
            headers = null;
            mapping = null;
            path = null;
            server.remove(this);
            connected = false;
        } catch (InvocationTargetException | IllegalAccessException | IOException e) {
            logger.error(e);
        }
        return Thread.currentThread();
    }

}

