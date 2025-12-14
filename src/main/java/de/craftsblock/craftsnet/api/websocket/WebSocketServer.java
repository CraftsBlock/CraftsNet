package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.codec.registry.TypeEncoderRegistry;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeEncoder;
import de.craftsblock.craftsnet.builder.ActivateType;
import de.craftsblock.craftsnet.api.ssl.SSL;
import org.jetbrains.annotations.ApiStatus;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * The WebSocketServer class represents a simple WebSocket server implementation. It allows WebSocket clients to connect,
 * manages their connections, and enables sending messages to connected clients.
 * The server can be configured to use SSL encryption by providing the necessary SSL key file.
 * It uses a ServerSocket to listen for incoming connections on the specified port.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.5
 * @see WebSocketClient
 * @since 2.1.1-SNAPSHOT
 */
public class WebSocketServer extends Server {

    private final ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private final ThreadPoolExecutor executor;
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketClient>> connected;

    private final TypeEncoderRegistry<WebSocketSafeTypeEncoder<?, ?>> typeEncoderRegistry = new TypeEncoderRegistry<>();

    private Thread connector;
    private ServerSocket serverSocket;

    private boolean shouldFragment = false;
    private int fragmentSize = 1024;

    /**
     * Constructs a WebSocketServer instance with the specified port number.
     *
     * @param craftsNet The CraftsNet instance which instantiates this websocket server.
     * @param port      The port number on which the server will listen for incoming connections.
     * @param ssl       A boolean flag indicating whether SSL encryption should be used (true for HTTPS, false for HTTP).
     */
    public WebSocketServer(CraftsNet craftsNet, int port, boolean ssl) {
        this(craftsNet, port, 0, ssl);
    }

    /**
     * Constructs a WebSocketServer instance with the specified port number and backlog size.
     *
     * @param craftsNet The CraftsNet instance which instantiates this websocket server.
     * @param port      The port number on which the server will listen for incoming connections.
     * @param backlog   The size of the backlog for the server socket.
     * @param ssl       A boolean flag indicating whether SSL encryption should be used.
     */
    public WebSocketServer(CraftsNet craftsNet, int port, int backlog, boolean ssl) {
        super(craftsNet, port, backlog, ssl);
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(r -> {
            Thread thread = threadFactory.newThread(r);
            String oldName = thread.getName();
            thread.setName("CraftsNet WebSocket-" + oldName.substring(oldName.lastIndexOf('-') + 1));
            return thread;
        });
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
        if (logger != null) logger.info("Web socket server bound to port %s", port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start() {
        if (running) return;

        try {
            logger.info("Starting websocket server on port %s", port);
            if (ssl) {
                SSLContext sslContext = SSL.load(this.craftsNet);
                if (sslContext != null) {
                    SSLServerSocket sslServerSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(port, backlog);
                    sslServerSocket.setSSLParameters(sslContext.getDefaultSSLParameters());
                    sslServerSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
                    serverSocket = sslServerSocket;
                }
            }
        } catch (IOException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException |
                 KeyStoreException | CertificateException e) {
            logger.error(e);
        } finally {
            if (serverSocket == null) {
                if (ssl)
                    logger.warning("SSl was not activated properly, using an socket server as fallback!");

                try {
                    serverSocket = new ServerSocket(port, backlog);
                } catch (IOException e) {
                    logger.error("Error while creating the %s socket server.", ssl ? "fallback" : "");
                    logger.error(e);
                }
            }
        }

        if (serverSocket == null) return;

        try {
            serverSocket.setSoTimeout(0);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        connected = new ConcurrentHashMap<>();

        connector = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    Socket socket = serverSocket.accept();

                    socket.setSoTimeout(1000 * 60 * 5);
                    socket.setKeepAlive(true);
                    socket.setTcpNoDelay(true);

                    if (socket instanceof SSLSocket sslSocket) {
                        sslSocket.addHandshakeCompletedListener(event -> connectClient(event.getSocket()));
                        sslSocket.startHandshake();
                    } else connectClient(socket);
                } catch (SocketException | SocketTimeoutException socketException) {
                    try {
                        if (Thread.currentThread().isInterrupted()) return;
                        Thread.sleep(15);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        });
        connector.setName("CraftsNet WS Acceptor");
        connector.start();

        super.start();
    }

    /**
     * Opens a new thread for the connection of the client and starts the websocket client.
     *
     * @param socket The socket used to connect.
     */
    private void connectClient(Socket socket) {
        executor.execute(new WebSocketClient(this.craftsNet, socket, this));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop() {
        if (!running) return;

        try {
            connected.values().stream().flatMap(ConcurrentLinkedQueue::stream)
                    .forEach(client -> {
                        try {
                            if (client.isConnected()) client.close(ClosureCode.GOING_AWAY, "Server closed!");
                            client.disconnect();
                        } catch (IllegalStateException ignored) {
                        }
                    });
            connected.clear();
            serverSocket.close();
            if (connector != null)
                connector.interrupt();

            serverSocket = null;
        } catch (IOException e) {
            logger.error(e);
        } finally {
            super.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void awakeOrWarn() {
        if (!isRunning() && isEnabled())
            // Start the web socket server as it is needed and currently not running
            this.craftsNet.getWebSocketServer().start();
        else if (!isEnabled())
            // Print a warning if the web socket server is disabled and socket endpoints has been registered
            logger.warning("A socket endpoint has been registered, but the web socket server is disabled!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void sleepIfNotNeeded() {
        if (isRunning() && !craftsNet.getRouteRegistry().hasWebsockets() && isStatus(ActivateType.DYNAMIC))
            stop();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return !isStatus(ActivateType.DISABLED);
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
     * Checks if SSL should be enabled for the server.
     *
     * @return {@code true} if SSL should be enabled, {@code false} otherwise.
     */
    public boolean shouldUseSSL() {
        return super.isSSL();
    }

    /**
     * Checks if the websocket server has a certain activation status in the builder.
     *
     * @param type The activation which should be present.
     * @return true if the activation status is equals, false otherwise.
     */
    private boolean isStatus(ActivateType type) {
        return craftsNet.getBuilder().isWebSocketServer(type);
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
     * Returns whether fragmentation is enabled or not.
     *
     * @return true if fragmentation is enabled, false otherwise
     */
    @ApiStatus.Experimental
    public synchronized boolean shouldFragment() {
        return shouldFragment;
    }

    /**
     * Enable or disable fragmentation of messages send by the server.
     *
     * @param shouldFragment true if fragmentation should be enabled, false otherwise.
     */
    @ApiStatus.Experimental
    public synchronized void setFragmentationEnabled(boolean shouldFragment) {
        this.shouldFragment = shouldFragment;
    }

    /**
     * Returns the size which should every fragment of a frame should have.
     *
     * @return The max size of each frame.
     */
    @ApiStatus.Experimental
    public synchronized int getFragmentSize() {
        return fragmentSize;
    }

    /**
     * Sets the maximum size of each fragment of a frame.
     *
     * @param fragmentSize The max size of the fragments.
     */
    @ApiStatus.Experimental
    public synchronized void setFragmentSize(int fragmentSize) {
        if (fragmentSize <= 0) return;
        this.fragmentSize = fragmentSize;
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
        if (connected.containsKey(path)) connected.get(path).forEach(client -> client.sendMessage(data));
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
                    if (entry.getValue().isEmpty()) connected.remove(entry.getKey());

                    if (!client.isConnected()) return;
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

}