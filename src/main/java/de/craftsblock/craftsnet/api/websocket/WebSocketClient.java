package de.craftsblock.craftsnet.api.websocket;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry.EndpointMapping;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareCallbackInfo;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareRegistry;
import de.craftsblock.craftsnet.api.middlewares.WebsocketMiddleware;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.requirements.Requirement;
import de.craftsblock.craftsnet.api.session.Session;
import de.craftsblock.craftsnet.api.utils.Context;
import de.craftsblock.craftsnet.api.transformers.TransformerPerformer;
import de.craftsblock.craftsnet.api.utils.ProtocolVersion;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.api.websocket.annotations.ApplyDecoder;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeDecoder;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtension;
import de.craftsblock.craftsnet.events.sockets.ClientConnectEvent;
import de.craftsblock.craftsnet.events.sockets.ClientDisconnectEvent;
import de.craftsblock.craftsnet.events.sockets.message.IncomingSocketMessageEvent;
import de.craftsblock.craftsnet.events.sockets.message.OutgoingSocketMessageEvent;
import de.craftsblock.craftsnet.events.sockets.message.ReceivedPingMessageEvent;
import de.craftsblock.craftsnet.events.sockets.message.ReceivedPongMessageEvent;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @version 3.7.1
 * @see WebSocketServer
 * @since 2.1.1-SNAPSHOT
 */
public class WebSocketClient implements Runnable, RequireAble {

    private static final String MESSAGE_TRIED_CONNECTING_ERROR = "%s tried to connect to %s \u001b[38;5;9m[%s]";

    private static final String WEBSOCKET_HANDSHAKE_MAGIC_TEXT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final MessageDigest handshakeDigest;

    static {
        try {
            // Set the digest algorithm for building the handshakes
            handshakeDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final WebSocketServer server;
    private final Socket socket;
    private final Session session;
    private final Scheme scheme;
    private final List<WebSocketExtension> extensions;
    private final TransformerPerformer transformerPerformer;
    private final Map<String, Matcher> matchers;

    private SocketExchange exchange;
    private ProtocolVersion protocolVersion;
    private Headers headers;
    private String ip;
    private String path;
    private String domain;
    private EnumMap<ProcessPriority.Priority, List<EndpointMapping>> mappings;

    private BufferedReader reader;
    private OutputStream writer;
    private final Object writerLock = new Object();
    private final ByteArrayOutputStream readBuffer = new ByteArrayOutputStream();

    private final CraftsNet craftsNet;
    private final Logger logger;

    private boolean active = false;
    private boolean connected = false;

    private boolean shouldMaskOutgoing = false;
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
        this.session = new Session();
        this.scheme = Scheme.WS.getSsl(server.isSSL());
        this.extensions = new ArrayList<>();

        this.shouldFragment = server.shouldFragment();
        this.fragmentSize = -1;
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.getLogger();

        // Create a transformer performer which handles all transformers
        this.transformerPerformer = new TransformerPerformer(this.craftsNet, 2, e -> {
            sendMessage(Json.empty().set("error", "Could not process transformer: " + e.getMessage()).toString());
            disconnect();
        });
        this.matchers = new HashMap<>();
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

        this.active = true;
        try {
            // Setup input and output streams for communication with the client
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = socket.getOutputStream();
            headers = readHeaders(); // Read and store the client's headers for later use

            int secWebsocketVersion = headers.containsKey("Sec-WebSocket-Version") ? Integer.parseInt(headers.getFirst("Sec-WebSocket-Version")) : 0;
            this.protocolVersion = new ProtocolVersion(this.scheme, secWebsocketVersion, 0);

            // Create a SocketExchange object to handle communication with the server
            this.exchange = new SocketExchange(new Context(), this.protocolVersion, this.server, this);

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
            if (getHeader("Sec-websocket-extensions") != null)
                for (String extension : getHeader("Sec-websocket-extensions").split(";\\s+"))
                    if (craftsNet.getWebSocketExtensionRegistry().hasExtension(extension))
                        this.extensions.add(craftsNet.getWebSocketExtensionRegistry().getExtensionByName(extension));

            // Send a WebSocket handshake to establish the connection
            sendHandshake();
            this.connected = true;

            // Reverse the extension list as it is required to process the more important one's at the end
            Collections.reverse(extensions);

            // Load the endpoint mappings
            this.mappings = getEndpoint();

            // Trigger the ClientConnectEvent to handle the client connection
            ClientConnectEvent event = new ClientConnectEvent(exchange);
            craftsNet.getListenerRegistry().call(event);

            // If the event is cancelled, disconnect the client
            if (event.isCancelled()) {
                if (event.hasCancelReason())
                    sendMessage(event.getCancelReason());
                disconnect();
                logger.debug(MESSAGE_TRIED_CONNECTING_ERROR, ip, path, event.getCancelReason());
                return;
            }

            // Handle middleware on connect
            MiddlewareCallbackInfo callbackInfo = performForEachAvailableMiddleware(
                    (info, middleware) -> middleware.handleConnect(info, exchange)
            );

            if (callbackInfo.isCancelled()) {
                if (callbackInfo.hasCancelReason())
                    sendMessage(callbackInfo.getCancelReason());
                disconnect();
                logger.debug(MESSAGE_TRIED_CONNECTING_ERROR, ip, path, callbackInfo.getCancelReason());
                return;
            }

            // Check if the requested path has a corresponding endpoint registered in the server
            if (!event.isAllowedWithoutMapping() && (mappings == null || mappings.isEmpty())) {
                // If the requested path has no corresponding endpoint, send an error message
                logger.debug("%s connected to %s \u001b[38;5;9m[NOT FOUND]", ip, path);
                closeInternally(ClosureCode.BAD_GATEWAY, Json.empty().set("error", "Path do not match any API endpoint!").toString(), true);
                return;
            }

            // Add this WebSocket client to the server's collection and mark it as connected
            server.add(path, this);

            // If the event is not cancelled, process incoming messages from the client
            logger.info("%s connected to %s", ip, path);

            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                Frame frame = readMessage();
                if (handleIncomingMessage(frame))
                    break;
            }
        } catch (SocketException ignored) {
        } catch (Throwable t) {
            if (t instanceof IOException ioException && ioException.getMessage().contains("EOF")) {
                logger.error("EOF: No more data can be read from the input streams!");
                return;
            }
            createErrorLog(t);
        } finally {
            disconnect();
        }
    }

    /**
     * Handles the frame of an incoming message.
     *
     * @param frame The frame of the incoming message.
     * @return {@code true} if the read loop should be exited, {@code false} otherwise.
     * @since 3.4.0-SNAPSHOT
     */
    private boolean handleIncomingMessage(Frame frame) {
        if (frame == null || frame.getOpcode().isUnknown()) {
            logger.warning("Received invalid websocket packet!");
            return true;
        }

        if (!this.connected || !this.socket.isConnected()) return true;
        if (isCloseFrame(frame) || validateOrClose(frame))
            return true;

        // Fire an incoming socket message event and continue if it was cancelled
        IncomingSocketMessageEvent incomingMessageEvent = new IncomingSocketMessageEvent(exchange, frame);
        craftsNet.getListenerRegistry().call(incomingMessageEvent);
        if (incomingMessageEvent.isCancelled()) return false;

        // Handle middlewares
        MiddlewareCallbackInfo callbackInfo = performForEachAvailableMiddleware(
                (info, middleware) -> middleware.handleMessageReceived(info, exchange, frame)
        );
        if (callbackInfo.isCancelled())
            return true;

        if (mappings == null || mappings.isEmpty()) return false;

        mappings.keySet().stream()
                .map(mappings::get)
                .flatMap(Collection::stream)
                .forEach(mapping -> this.handleMapping(mapping, frame));

        // Clear up transformer cache to free up memory
        transformerPerformer.clearCache();
        matchers.clear();
        return false;
    }

    /**
     * Process / Checks some things based on the {@link Opcode} of a frame.
     *
     * @param frame The frame to process / check.
     * @return {@code true} if the read loop should be ended, {@code false} otherwise.
     * @since 3.4.0-SNAPSHOT
     */
    private boolean validateOrClose(Frame frame) {
        return switch (frame.getOpcode()) {
            case PING -> {
                craftsNet.getListenerRegistry().call(new ReceivedPingMessageEvent(exchange, frame));
                yield false;
            }

            case PONG -> {
                craftsNet.getListenerRegistry().call(new ReceivedPongMessageEvent(exchange, frame));
                yield false;
            }

            case TEXT -> {
                if (Utils.isEncodingValid(frame.getData(), StandardCharsets.UTF_8)) yield false;
                closeInternally(ClosureCode.UNSUPPORTED_PAYLOAD, "Send byte values are not utf8 valid!", true);
                yield true;
            }

            default -> false;
        };
    }

    /**
     * Checks if the provided frame is a close frame.
     *
     * @param frame The {@link Frame} to check.
     * @return {@code true} if the frame is a close frame, {@code false} otherwise.
     * @since 3.4.0-SNAPSHOT
     */
    private boolean isCloseFrame(Frame frame) {
        byte @NotNull [] data = frame.getData();

        if (!frame.getOpcode().equals(Opcode.CLOSE)) return false;
        if (data.length <= 2) return true;

        closeCode = (data[0] & 0xFF) << 8 | (data[1] & 0xFF);
        closeReason = new String(Arrays.copyOfRange(data, 2, data.length));
        closeInternally(ClosureCode.NORMAL, "Acknowledged close", false);
        return true;
    }

    /**
     * Passes a socket message frame down to one specific endpoint.
     *
     * @param mapping The {@link EndpointMapping mapping} which should be handled.
     * @param frame   The {@link Frame} received.
     * @since 3.4.0-SNAPSHOT
     */
    private void handleMapping(EndpointMapping mapping, Frame frame) {
        try {
            if (!(mapping.handler() instanceof SocketHandler handler)) return;
            Method method = mapping.method();

            Pattern validator = mapping.validator();
            Matcher matcher = matchers.computeIfAbsent(mapping.validator().pattern(), pattern -> {
                Matcher fresh = validator.matcher(path);

                if (!fresh.matches()) {
                    sendMessage(Json.empty()
                            .set("error", "There was an unexpected error while matching!")
                            .toString());
                }

                return fresh;
            });
            transformerPerformer.setValidator(validator);

            // Extract and pass the message parameters to the endpoint handler
            Object[] args = new Object[matcher.groupCount() + 1];
            args[0] = exchange;
            args[1] = frame.getData();
            for (int i = 2; i <= matcher.groupCount(); i++) args[i] = matcher.group(i);

            if (processRequirements(mapping, frame)) return;

            // Perform all transformers and continue if the transformers exit with an exception
            if (!transformerPerformer.perform(mapping.handler(), method, args))
                return;

            preprocessMethodParameters(method, frame, args);

            // Invoke the handler method
            Object result = ReflectionUtils.invokeMethod(handler, method, args);
            if (result == null || !isConnected() || !isActive()) return;

            this.sendMessage(result);
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected exception whilst handling websocket mappings", t);
        }
    }

    /**
     * Processes the requirements for a specific {@link EndpointMapping mapping}.
     *
     * @param mapping The {@link EndpointMapping} to check.
     * @param frame   The {@link Frame} representing the incoming message.
     * @return {@code true} if there is a requirement mismatch, {@code false} otherwise.
     * @since 3.5.0
     */
    private boolean processRequirements(EndpointMapping mapping, Frame frame) {
        if (!craftsNet.getRequirementRegistry().getRequirements().containsKey(WebSocketServer.class)) return false;

        for (var requirementLink : craftsNet.getRequirementRegistry().getRequirementMethodLinks(WebSocketServer.class))
            try {
                Class<? extends Annotation> requirementAnnotation = requirementLink.requirement().getAnnotation();
                if (!mapping.isPresent(requirementAnnotation)) continue;
                if (!TypeUtils.isAssignable(Frame.class, requirementLink.arg())) continue;

                Requirement<? super RequireAble> requirement = requirementLink.requirement();
                return !requirement.applies(frame, mapping);
            } catch (NullPointerException | AssertionError ignored) {
            }

        return false;
    }

    /**
     * Preprocesses the parameters for a given WebSocket handler method by resolving and decoding
     * the second argument based on the method signature or annotations.
     * <p>
     * If the method is annotated with {@link ApplyDecoder}, the specified {@link WebSocketSafeTypeDecoder}
     * is instantiated and used to decode the {@link Frame} into the expected parameter type.
     * Otherwise, the second parameter is automatically filled with a default interpretation based on its type:
     * <ul>
     *     <li>{@link String} -> UTF-8 decoded string from frame data</li>
     *     <li>{@link Frame} -> Cloned {@link Frame} instance</li>
     *     <li>{@link ByteBuffer} -> Raw buffer from the frame</li>
     * </ul>
     *
     * @param method The handler method whose parameters are being prepared.
     * @param frame  The incoming {@link Frame} containing WebSocket data.
     * @param args   The argument array to be passed to the method (modified in-place).
     * @since 3.5.0
     */
    private void preprocessMethodParameters(Method method, Frame frame, Object[] args) {
        if (method.getParameterCount() < 2) return;

        ApplyDecoder applyDecoder = method.getAnnotation(ApplyDecoder.class);
        if (applyDecoder != null) {
            Class<? extends WebSocketSafeTypeDecoder<?>> decoderType = applyDecoder.value();
            WebSocketSafeTypeDecoder<?> decoder = ReflectionUtils.getNewInstance(decoderType);

            args[1] = decoder.decode(frame);
            return;
        }

        // @FixMe: Using switch when upgrading to java 21+
        args[1] = switch (method.getParameterTypes()[1].getName()) {
            case "java.lang.String" -> new String(frame.getData(), StandardCharsets.UTF_8);
            case "de.craftsblock.craftsnet.api.websocket.Frame" -> frame.clone();
            case "de.craftsblock.craftsnet.utils.ByteBuffer" -> frame.getBuffer();
            default -> args[1];
        };
    }

    /**
     * Creates an error log for a specific throwable
     *
     * @param t the throwable which has been thrown
     */
    private void createErrorLog(Throwable t) {
        Json message = Json.empty().set(
                "error.message", "An unexpected exception happened whilst processing your message!"
        );

        if (craftsNet.getLogStream() != null) {
            long errorID = craftsNet.getLogStream().createErrorLog(this.craftsNet, t, this.scheme.getName(), path);
            logger.error("Error: %s", t, errorID);
            message.set("error.identifier", errorID);
        } else logger.error(t);

        if (isConnected() && isActive()) sendMessage(message);
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
                for (HttpMethod method : HttpMethod.ALL.getMethods())
                    if (line.startsWith(method.name())) {
                        path = line.split(" ")[1].replaceAll("//+", "/");
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
    public EnumMap<ProcessPriority.Priority, List<EndpointMapping>> getEndpoint() {
        return this.mappings != null ? mappings : craftsNet.getRouteRegistry().getSocket(this);
    }

    /**
     * Sends a WebSocket handshake to the client to establish the connection.
     * The handshake includes the required headers for a WebSocket upgrade.
     */
    private void sendHandshake() {
        try {
            String concatenated = getHeader("Sec-WebSocket-Key") + WEBSOCKET_HANDSHAKE_MAGIC_TEXT;
            byte[] hash = handshakeDigest.digest(concatenated.getBytes(StandardCharsets.UTF_8));

            String extensions = String.join(",", this.extensions.parallelStream().map(WebSocketExtension::getProtocolName).toList());
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + Base64.getEncoder().encodeToString(hash) + "\r\n"
                    + (!extensions.isEmpty() ? "Sec-websocket-extensions: " + extensions + "\r\n" : "")
                    + "\r\n";

            this.sendMessageRaw(response.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        while (isConnected() && frame.get() == null || !frame.get().isFinalFrame()) {
            Frame read = Frame.read(inputStream, readBuffer);

            if (read.getOpcode().isUnknown()) {
                closeInternally(ClosureCode.PROTOCOL_ERROR, "Unknown opcode!", true);
                throw new IOException("Unknown opcode received!");
            }

            if (frame.get() == null) {
                frame.set(read);
                if (read.isFinalFrame()) break;
                continue;
            }

            frame.get().appendFrame(read);
        }

        // Return, when the frame is null
        if (frame.get() == null) return null;

        if (this.extensions.isEmpty()) return frame.get();

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
    public Session getSession() {
        return session;
    }

    /**
     * Returns whether the outgoing messages should be masked or not.
     *
     * @return {@code true} if the outgoing messages should be masked, {@code false} otherwise.
     */
    public boolean shouldMaskOutgoing() {
        return shouldMaskOutgoing;
    }

    /**
     * Sets whether the outgoing message should be masked or not.
     *
     * @param shouldMaskOutgoing {@code true} if the outgoing messages should be masked, {@code false} otherwise.
     */
    public void setMaskingOutgoing(boolean shouldMaskOutgoing) {
        this.shouldMaskOutgoing = shouldMaskOutgoing;
    }

    /**
     * Returns whether fragmentation is enabled or not.
     *
     * @return true if fragmentation is enabled, false otherwise
     */
    @ApiStatus.Experimental
    public boolean shouldFragment() {
        return server.shouldFragment() || shouldFragment;
    }

    /**
     * Enable or disable fragmentation of messages send by the server.
     *
     * @param shouldFragment true if fragmentation should be enabled, false otherwise.
     */
    @ApiStatus.Experimental
    public void setFragmentationEnabled(boolean shouldFragment) {
        this.shouldFragment = shouldFragment;
    }

    /**
     * Returns the size which should every fragment of a frame should have.
     *
     * @return The max size of each frame.
     */
    @ApiStatus.Experimental
    public int getFragmentSize() {
        if (fragmentSize <= 0) return server.getFragmentSize();
        return fragmentSize;
    }

    /**
     * Sets the maximum size of each fragment of a frame.
     *
     * @param fragmentSize The max size of the fragments.
     */
    @ApiStatus.Experimental
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
     * @param data The message to be sent, as a bytebuffer.
     */
    public void sendMessage(ByteBuffer data) {
        sendMessage(data.getSource(), Opcode.BINARY);
    }

    /**
     * Sends a message to the connected WebSocket client.
     *
     * @param data The message to be sent, as an array of bytes.
     */
    public void sendMessage(byte[] data) {
        sendMessage(data, Opcode.BINARY);
    }

    /**
     * Sends a message to the connected WebSocket client.
     * <p>
     * This method will try to parse the object in the following order:
     * <ol>
     *     <li>{@link String}</li>
     *     <li>{@code byte[]}</li>
     *     <li>{@link Json}</li>
     *     <li>{@link ByteBuffer}</li>
     * </ol>
     * If none of these types can be applied, the object is converted
     * into a string with {@link Object#toString()} and then sent.
     *
     * @param data The message to be sent, as an object.
     * @since 3.4.3
     */
    public void sendMessage(Object data) {
        // @FixMe: Using switch when upgrading to java 21+

        if (data instanceof String string) this.sendMessage(string);
        else if (data instanceof byte[] bytes) this.sendMessage(bytes);
        else if (data instanceof Json json) this.sendMessage(json);
        else if (data instanceof ByteBuffer buffer) this.sendMessage(buffer);
        else {
            // Check for encoders
            var encoders = server.getTypeEncoderRegistry();
            Class<?> type = data.getClass();
            if (encoders.hasCodec(type)) {
                var codecLink = encoders.getLinkedCodecMethod(type);

                if (codecLink != null) {
                    var result = ReflectionUtils.invokeMethod(codecLink.codec(), codecLink.method(), data);
                    this.sendMessage(result);
                    return;
                }
            }

            // Convert the object to string when no encoder is present
            this.sendMessage(data.toString());
        }
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
    public void close(@Range(from = 1000, to = 4999) int code, String reason) {
        if (ClosureCode.isInternal(code))
            throw new IllegalArgumentException("Invalid close code %s: not allowed to use internal close codes!".formatted(
                    code
            ));

        if (code < 1000 || code > 4999) {
            closeInternally(ClosureCode.SERVER_ERROR, "Used close code " + code, true);
            throw new IllegalArgumentException("Invalid close code " + code + ": must be between 1000–4999!");
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
    private void closeInternally(int code, String reason, boolean closeByServer) {
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
    private void sendMessage(byte[] data, Opcode opcode) {
        if (!isConnected())
            throw new IllegalStateException("The websocket connection has already been closed!");
        if (writer == null)
            throw new IllegalStateException("The websocket writer has already been closed!");

        try {
            if (data == null || data.length == 0) {
                byte[] subject = new byte[2];
                subject[0] = (byte) (0x80 | opcode.byteValue());
                // subject[1] is already 0x00 so no further write is required

                this.sendMessageRaw(subject);
                return;
            }

            Frame frame = new Frame(true, false, false, false, false, opcode, data);
            frame.setMasked(this.shouldMaskOutgoing);

            OutgoingSocketMessageEvent event = new OutgoingSocketMessageEvent(exchange, frame);
            if (!opcode.equals(Opcode.CLOSE) && !opcode.equals(Opcode.CONTINUATION)) {
                craftsNet.getListenerRegistry().call(event);
                if (event.isCancelled())
                    return;

                // Handle middlewares
                MiddlewareCallbackInfo callbackInfo = performForEachAvailableMiddleware(
                        (info, middleware) -> middleware.handleMessageSent(info, exchange, frame)
                );
                if (callbackInfo.isCancelled())
                    return;
            }

            Frame subject = event.getFrame();
            if (shouldFragment()) {
                Collection<Frame> frames = subject.fragmentFrame(getFragmentSize());
                this.sendMessageFrames(frames.toArray(Frame[]::new));
                return;
            }

            this.sendMessageFrames(subject);
        } catch (SocketException ignored) {
        } catch (IOException e) {
            disconnect();
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends specific message {@link Frame frames} to the client.
     *
     * @param frames An array of {@link Frame frames} that should be sent.
     * @throws IOException If an IO error occurs while sending the frame.
     * @since 3.4.0-SNAPSHOT
     */
    private void sendMessageFrames(Frame... frames) throws IOException {
        for (Frame frame : frames) {
            for (WebSocketExtension extension : this.extensions)
                frame = extension.encode(frame);

            synchronized (this.writerLock) {
                frame.write(this.writer);
            }
        }
    }

    /**
     * Thread safe wrapper for sending bytes to the client.
     *
     * @param data The bytes that should be sent.
     * @throws IOException If an IO error occurs while sending the bytes.
     * @since 3.4.0-SNAPSHOT
     */
    private void sendMessageRaw(byte[] data) throws IOException {
        synchronized (this.writerLock) {
            this.writer.write(data);
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
        if (this.socket == null || this.writer == null || this.reader == null) return false;
        return this.connected && this.socket.isConnected();
    }

    /**
     * Disconnects the WebSocket client and performs necessary cleanup operations.
     * This method triggers the ClientDisconnectEvent before closing the socket and removing the client from the server.
     */
    protected synchronized void disconnect() {
        if (!this.connected && !socket.isConnected()) return;
        if (reader == null || writer == null) return;

        try {
            reader.close();
            reader = null;
            writer.close();
            writer = null;

            if (socket != null) socket.close();

            craftsNet.getListenerRegistry().call(new ClientDisconnectEvent(exchange, closeCode, closeReason, closeByServer));

            if (!closeByServer && this.connected) logger.warning("%s disconnected abnormal: The underlying tcp connection has been killed!", ip);
            else if (!closeByServer && closeCode != -1 && closeCode != ClosureCode.NORMAL.intValue()) {
                ClosureCode code = ClosureCode.fromInt(closeCode);
                logger.warning("%s disconnected abnormal (Code: %s)%s",
                        ip, code != null ? code : closeCode, closeReason != null && !closeReason.isEmpty() ? ": " + closeReason : "");
            } else
                logger.debug("%s disconnected", ip);

            performForEachAvailableMiddleware(
                    (info, middleware) -> middleware.handleDisconnect(info, exchange)
            );

            headers = null;
            mappings = null;
            transformerPerformer.clearCache();
            matchers.clear();
            session.clear();
            extensions.clear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            server.remove(this);
            this.connected = false;
        }
    }

    /**
     * Executes the given {@link BiConsumer} for all available
     * {@link WebsocketMiddleware} instances, including:
     * <ul>
     *   <li>Globally registered middlewares from
     *       {@link MiddlewareRegistry#getMiddlewares(Class)} for {@link WebSocketServer}.</li>
     *   <li>Middlewares defined in {@link EndpointMapping EndpointMappings}
     *       via {@link EndpointMapping#middlewares()}.</li>
     * </ul>
     *
     * @param consumer The operation to be applied to each {@link WebsocketMiddleware}.
     * @return A new {@link MiddlewareCallbackInfo} instance serving as callback context.
     * @since 3.5.3
     */
    private MiddlewareCallbackInfo performForEachAvailableMiddleware(BiConsumer<MiddlewareCallbackInfo, WebsocketMiddleware> consumer) {
        MiddlewareCallbackInfo callbackInfo = new MiddlewareCallbackInfo();

        craftsNet.getMiddlewareRegistry().getMiddlewares(WebSocketServer.class).forEach(middleware -> {
            if (middleware instanceof WebsocketMiddleware websocketMiddleware)
                consumer.accept(callbackInfo, websocketMiddleware);
        });

        this.mappings.values().forEach(mappingList -> mappingList.forEach(
                mapping -> mapping.middlewares().forEach(
                        middleware -> {
                            if (middleware instanceof WebsocketMiddleware websocketMiddleware)
                                consumer.accept(callbackInfo, websocketMiddleware);
                        })
        ));

        return callbackInfo;
    }

}

