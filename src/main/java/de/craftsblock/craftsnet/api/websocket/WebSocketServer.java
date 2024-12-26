package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftscore.annotations.Experimental;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.builder.ActivateType;
import de.craftsblock.craftsnet.utils.SSL;

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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The WebSocketServer class represents a simple WebSocket server implementation. It allows WebSocket clients to connect,
 * manages their connections, and enables sending messages to connected clients.
 * The server can be configured to use SSL encryption by providing the necessary SSL key file.
 * It uses a ServerSocket to listen for incoming connections on the specified port.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
 * @see WebSocketClient
 * @since 2.1.1-SNAPSHOT
 */
public class WebSocketServer extends Server {

    private ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketClient>> connected;

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
    }

    /**
     * {@inheritDoc}
     *
     * @param port    {@inheritDoc}
     * @param backlog {@inheritDoc}
     */
    @Override
    public void bind(int port, int backlog) {
        super.bind(port, backlog);
        if (logger != null) logger.info("Web socket server bound to port " + port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        try {
            logger.info("Starting websocket server on port " + port);
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
                    logger.error("Error while creating the " + (ssl ? "fallback" : "") + " socket server.");
                    logger.error(e);
                }
            }
        }

        if (serverSocket == null) return;

        connected = new ConcurrentHashMap<>();

        connector = new Thread(() -> {
            AtomicInteger i = new AtomicInteger();
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    Socket socket = serverSocket.accept();

                    // Set timeout duration
                    socket.setSoTimeout(1000 * 60 * 5);

                    if (socket instanceof SSLSocket sslSocket) {
                        sslSocket.addHandshakeCompletedListener(event -> connectClient(event.getSocket(), i));
                        sslSocket.startHandshake();
                    } else connectClient(socket, i);
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        });
        connector.setName("Websocket - Connector");
        connector.start();

        super.start();
    }

    /**
     * Opens a new thread for the connection of the client and starts the websocket client.
     *
     * @param socket The socket used to connect.
     * @param i      The current identifier of the websocket client.
     */
    private void connectClient(Socket socket, AtomicInteger i) {
        Thread client = new Thread(new WebSocketClient(this.craftsNet, socket, this));
        client.setName("Websocket#" + i.getAndIncrement());
        client.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        try {
            connected.values().stream().flatMap(ConcurrentLinkedQueue::stream)
                    .forEach(client -> {
                        try {
                            if (client.isConnected()) client.close(ClosureCode.GOING_AWAY, "Server closed!");
                            client.disconnect().interrupt();
                        } catch (IllegalStateException ignored) {
                        }
                    });
            connected.clear();
            serverSocket.close();
            if (connector != null)
                connector.interrupt();
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
    public void awakeOrWarn() {
        if (!isRunning() && isEnabled())
            // Start the web socket server as it is needed and currently not running
            this.craftsNet.webSocketServer().start();
        else if (!isEnabled())
            // Print a warning if the web socket server is disabled and socket endpoints has been registered
            logger.warning("A socket endpoint has been registered, but the web socket server is disabled!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sleepIfNotNeeded() {
        if (isRunning() && !craftsNet.routeRegistry().hasWebsockets() && isStatus(ActivateType.DYNAMIC))
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
     * Returns whether fragmentation is enabled or not.
     *
     * @return true if fragmentation is enabled, false otherwise
     */
    @Experimental
    public boolean shouldFragment() {
        return shouldFragment;
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
        return fragmentSize;
    }

    /**
     * Sets the maximum size of each fragment of a frame.
     *
     * @param fragmentSize The max size of the fragments.
     */
    @Experimental
    public void setFragmentSize(int fragmentSize) {
        if (fragmentSize <= 0) return;
        this.fragmentSize = fragmentSize;
    }

    /**
     * Sends a message to all connected WebSocket clients.
     *
     * @param data The message to be sent.
     */
    public void broadcast(String data) {
        connected.values().forEach(clients -> clients.forEach(client -> client.sendMessage(data)));
    }

    /**
     * Sends a message to all connected WebSocket clients with a specified path.
     *
     * @param path The path to which the clients are assigned.
     * @param data The message to be sent.
     */
    public void broadcast(String path, String data) {
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
                    try {
                        entry.getValue().removeIf(client::equals);
                        if (entry.getValue().isEmpty()) connected.remove(entry.getKey());

                        if (!client.isConnected()) return;
                        Thread thread = client.disconnect();
                        thread.interrupt();
                        thread.join(500);
                    } catch (InterruptedException ignored) {
                    }
                });
    }

    /**
     * Retrieves the list of all currently connected {@link WebSocketClient}s.
     *
     * @return The list of the connected {@link WebSocketClient}s.
     */
    public List<WebSocketClient> getClients() {
        return connected.values().stream().flatMap(Collection::stream).toList();
    }

}