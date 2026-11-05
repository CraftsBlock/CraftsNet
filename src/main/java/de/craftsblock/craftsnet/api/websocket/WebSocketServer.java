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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
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
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketClient>> connected = new ConcurrentHashMap<>();

    private Supplier<Executor> executorFactory = () -> Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("WebSocket-", 1).factory()
    );
    private Executor executor;

    private int readTimeout;

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

        this.readTimeout = this.getBuilder().readTimeout();

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
                executorFactory.get(),
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
                    if (socket instanceof SSLSocket sslSocket) {
                        sslSocket.startHandshake();
                    }

                    socket.setSoTimeout(readTimeout);

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
            connected.values().stream().flatMap(ConcurrentLinkedQueue::stream)
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
     * @param data The message to be sent.
     */
    public synchronized void broadcast(String data) {
        connected.values().forEach(clients -> clients.forEach(client -> client.sendMessage(data)));
    }

    /**
     * Sends a message to all connected WebSocket clients with a specified path.
     *
     * @param path The path to which the clients are assigned.
     * @param data The message to be sent.
     */
    public synchronized void broadcast(String path, String data) {
        if (connected.containsKey(path)) {
            connected.get(path).forEach(client -> client.sendMessage(data));
        }
    }

    /**
     * Adds a WebSocket client to a specified path.
     *
     * @param path   The path to which the client will be assigned.
     * @param client The WebSocket client that will be added.
     */
    protected void add(String path, WebSocketClient client) {
        connected.computeIfAbsent(path, s -> new ConcurrentLinkedQueue<>()).add(client);
    }

    /**
     * Removes a WebSocket client from the server and the associated path.
     *
     * @param client The WebSocket client that will be removed.
     */
    protected void remove(WebSocketClient client) {
        connected.entrySet().stream()
                .filter(entry -> entry.getValue().contains(client))
                .forEach(entry -> {
                    entry.getValue().removeIf(client::equals);
                    if (entry.getValue().isEmpty()) {
                        connected.remove(entry.getKey());
                    }

                    if (!client.isConnected()) {
                        return;
                    }

                    client.disconnect();
                });
    }

    /**
     * Retrieves the list of all currently connected {@link WebSocketClient}s.
     *
     * @return The list of the connected {@link WebSocketClient}s.
     */
    public List<WebSocketClient> getClients() {
        return connected.values().stream().flatMap(Collection::stream).distinct().toList();
    }

    /**
     * Sets the factory used to create the executor responsible for handling WebSocket connections.
     *
     * @param executorFactory The supplier providing the executor instance.
     */
    public void setExecutorFactory(@NotNull Supplier<Executor> executorFactory) {
        Objects.requireNonNull(executorFactory, "executorFactory");
        this.executorFactory = executorFactory;
    }

    /**
     * Retrieves the factory used to create the executor responsible for handling WebSocket connections.
     *
     * @return The supplier providing the executor instance.
     */
    public @NotNull Supplier<Executor> getExecutorFactory() {
        return executorFactory;
    }

}