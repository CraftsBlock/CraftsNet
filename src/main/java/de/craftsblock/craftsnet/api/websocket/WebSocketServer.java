package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.codec.registry.TypeEncoderRegistry;
import de.craftsblock.craftsnet.api.ssl.SSL;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeEncoder;
import de.craftsblock.craftsnet.builder.server.ServerState;
import de.craftsblock.craftsnet.builder.server.WebServerBuilder;
import de.craftsblock.craftsnet.builder.server.WebSocketServerBuilder;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * The WebSocketServer class represents a simple WebSocket server implementation. It allows WebSocket clients to connect,
 * manages their connections, and enables sending messages to connected clients.
 * The server can be configured to use SSL encryption by providing the necessary SSL key file.
 * It uses a ServerSocket to listen for incoming connections on the specified port.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see WebSocketClient
 * @since 2.1.1-SNAPSHOT
 */
public class WebSocketServer extends Server<WebSocketServerBuilder> {

    private final TypeEncoderRegistry<WebSocketSafeTypeEncoder<?, ?>> typeEncoderRegistry = new TypeEncoderRegistry<>();
    private final Map<String, Set<WebSocketClient>> connected = new ConcurrentHashMap<>();

    private Executor executor;

    private Thread connector;
    private ServerSocket serverSocket;

    /**
     * Constructs a WebSocketServer instance with the specified port number.
     *
     * @param craftsNet The CraftsNet instance which instantiates this websocket server.
     * @param builder   The {@link WebServerBuilder} which should build this web server.
     */
    public WebSocketServer(CraftsNet craftsNet, WebSocketServerBuilder builder) {
        super(craftsNet, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @param port    {@inheritDoc}
     * @param backlog {@inheritDoc}
     */
    @Override
    public synchronized void bind(int port, int backlog) {
        super.bind(port, backlog);
        this.getLogger().info("Web socket server bound to port %s", port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        try {
            this.getLogger().info("Starting websocket server on port %s", port);

            if (ssl) {
                SSLContext sslContext = SSL.load(this.getCraftsNet());
                if (sslContext == null) {
                    throw new IllegalStateException(
                            "SSL context could not be initialized"
                    );
                }

                serverSocket = sslContext.getServerSocketFactory()
                        .createServerSocket(port, backlog);
            } else {
                serverSocket = new ServerSocket(port, backlog);
            }
        } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException |
                 NoSuchAlgorithmException | CertificateException | IOException |
                 InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to initialize SSL context");
        }

        executor = Objects.requireNonNull(
                this.getExecutorFactory().apply(this.getBuilder()),
                "Executor factory returned null"
        );

        connector = Thread.ofPlatform()
                .name("CraftsNet WS Acceptor")
                .start(this::acceptConnections);

        super.start();
    }

    /**
     * Accepts incoming client connections and delegates them to the configured executor.
     * The method runs in a dedicated virtual thread until the server socket is closed
     * or the thread is interrupted.
     */
    private void acceptConnections() {
        while (!Thread.currentThread().isInterrupted()
                && !serverSocket.isClosed()) {

            try {
                Socket socket = serverSocket.accept();

                socket.setKeepAlive(true);
                socket.setTcpNoDelay(true);

                connectClient(socket);
            } catch (SocketException e) {
                if (serverSocket.isClosed()) {
                    return;
                }

                this.getLogger().error("Websocket accept failed", e);
            } catch (IOException e) {
                this.getLogger().error("Failed to accept websocket connection", e);
            }
        }
    }

    /**
     * Opens a new thread for the connection of the client and starts the websocket client.
     *
     * @param socket The socket used to connect.
     */
    private void connectClient(Socket socket) {
        try {
            executor.execute(() -> {
                try {
                    socket.setSoTimeout(5000);

                    if (socket instanceof SSLSocket sslSocket) {
                        sslSocket.startHandshake();
                    }

                    new WebSocketClient(this.getCraftsNet(), socket, this)
                            .run();
                } catch (Exception e) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }

                    this.getLogger().error("Failed to handle websocket connection", e);
                }
            });
        } catch (RejectedExecutionException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }

            this.getLogger().warning("Websocket connection rejected");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }

        try {
            connected.values().stream().flatMap(Set::stream)
                    .forEach(client -> {
                        try {
                            if (client.isConnected()) {
                                client.close(ClosureCode.GOING_AWAY, "Server closed!");
                            }

                            client.disconnect();
                        } catch (IllegalStateException ignored) {
                        }
                    });
            connected.clear();

            serverSocket.close();
            if (connector != null) {
                connector.interrupt();
            }

            if (executor instanceof AutoCloseable closeable) {
                closeable.close();
            }

            serverSocket = null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop the web socket server properly", e);
        } finally {
            super.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void awakeOrWarn() {
        if (!isRunning() && isEnabled()) {
            // Start the web socket server as it is needed and currently not running
            this.getCraftsNet().getWebSocketServer().start();
        } else if (!isEnabled()) {
            // Print a warning if the web socket server is disabled and socket endpoints has been registered
            this.getLogger().warning("A socket endpoint has been registered, but the web socket server is disabled!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void sleepIfNotNeeded() {
        if (this.isRunning() && !this.getCraftsNet().getRouteRegistry().hasWebsockets()
                && this.isStatus(ServerState.DYNAMIC)) {
            stop();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isSSL() {
        return this.serverSocket instanceof SSLServerSocket;
    }

    /**
     * Retrieves the {@link TypeEncoderRegistry} instance managing
     * the registration and lookup of {@link WebSocketSafeTypeEncoder} codecs.
     *
     * @return the {@link TypeEncoderRegistry} for {@link WebSocketSafeTypeEncoder} instances
     * @since 3.5.0
     */
    public TypeEncoderRegistry<WebSocketSafeTypeEncoder<?, ?>> getTypeEncoderRegistry() {
        return typeEncoderRegistry;
    }

    /**
     * Sends a message to all connected WebSocket clients.
     *
     * @param message The message to be sent.
     */
    public void broadcast(String message) {
        connected.values().forEach(clients -> this.broadcast(clients, message));
    }

    /**
     * Sends a message to all connected WebSocket clients with a specified path.
     *
     * @param path    The path to which the clients are assigned.
     * @param message The message to be sent.
     */
    public void broadcast(String path, String message) {
        if (connected.containsKey(path)) {
            this.broadcast(this.connected.get(path), message);
        }
    }

    /**
     * Sends a message to all specified WebSocket clients.
     *
     * @param clients The clients to which the broadcast should be sent.
     * @param message The message to be sent.
     */
    public void broadcast(Collection<WebSocketClient> clients, String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        for (WebSocketClient client : clients) {
            client.sendMessage(data, Opcode.TEXT);
        }
    }

    /**
     * Adds a WebSocket client to a specified path.
     *
     * @param path   The path to which the client will be assigned.
     * @param client The WebSocket client that will be added.
     */
    protected void add(String path, WebSocketClient client) {
        connected.computeIfAbsent(path, key -> ConcurrentHashMap.newKeySet()).add(client);
    }

    /**
     * Removes a WebSocket client from the server and the associated path.
     *
     * @param client The WebSocket client that will be removed.
     */
    protected void remove(WebSocketClient client) {
        Set<WebSocketClient> clients = connected.get(client.getPath());
        if (clients == null || !clients.contains(client)) {
            return;
        }

        clients.remove(client);
        if (clients.isEmpty()) {
            connected.remove(client.getPath(), clients);
        }

        if (client.isConnected()) {
            client.disconnect();
        }
    }

    /**
     * Retrieves the list of all currently connected {@link WebSocketClient}s.
     *
     * @return The list of the connected {@link WebSocketClient}s.
     */
    public List<WebSocketClient> getClients() {
        return connected.values().stream().flatMap(Collection::stream).distinct().toList();
    }

}
