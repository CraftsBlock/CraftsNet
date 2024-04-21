package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.logging.Logger;
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
 * @version 1.1
 * @see WebSocketClient
 * @since 2.1.1
 */
public class WebSocketServer extends Server {

    private final CraftsNet craftsNet;
    private final Logger logger;
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketClient>> connected;

    private Thread connector;
    private ServerSocket serverSocket;

    /**
     * Constructs a WebSocketServer instance with the specified port number.
     *
     * @param port The port number on which the server will listen for incoming connections.
     * @param ssl  A boolean flag indicating whether SSL encryption should be used (true for HTTPS, false for HTTP).
     */
    public WebSocketServer(int port, boolean ssl) {
        this(port, 0, ssl);
    }

    /**
     * Constructs a WebSocketServer instance with the specified port number and backlog size.
     *
     * @param port    The port number on which the server will listen for incoming connections.
     * @param backlog The size of the backlog for the server socket.
     * @param ssl     A boolean flag indicating whether SSL encryption should be used.
     */
    public WebSocketServer(int port, int backlog, boolean ssl) {
        super(port, backlog, ssl);
        this.craftsNet = CraftsNet.instance();
        this.logger = this.craftsNet.logger();
    }

    @Override
    public void bind(int port, int backlog) {
        super.bind(port, backlog);
        if (logger != null) logger.info("Web socket server bound to port " + port);
    }

    /**
     * Starts the WebSocket server and waits for incoming connections.
     */
    public void start() {
        try {
            logger.info("Starting websocket server on port " + port);
            if (ssl) {
                SSLContext sslContext = SSL.load();
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
        super.start();

        connector = new Thread(() -> {
            AtomicInteger i = new AtomicInteger();
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    Socket socket = serverSocket.accept();
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
        connector.setName("Websocket Server - Connector");
        connector.start();
    }

    private void connectClient(Socket socket, AtomicInteger i) {
        Thread client = new Thread(new WebSocketClient(socket, this));
        client.setName("Websocket#" + i.getAndIncrement());
        client.start();
    }

    /**
     * Stops the WebSocket server and closes all connections.
     */
    @Override
    public void stop() {
        try {
            connected.forEach((useless, client) -> client.forEach(webSocketClient -> webSocketClient.disconnect().interrupt()));
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

    @Override
    public void awakeOrWarn() {
        if (!isRunning() && isEnabled())
            // Start the web socket server as it is needed and currently not running
            this.craftsNet.webSocketServer().start();
        else
            // Print a warning if the web socket server is disabled and socket endpoints has been registered
            logger.warning("A socket endpoint has been registered, but the web socket server is disabled!");
    }

    @Override
    public void sleepIfNotNeeded() {
        if (isRunning() && !craftsNet.routeRegistry().hasWebsockets() && isStatus(CraftsNet.ActivateType.DYNAMIC))
            stop();
    }

    @Override
    public boolean isEnabled() {
        return !isStatus(CraftsNet.ActivateType.DISABLED);
    }

    private boolean isStatus(CraftsNet.ActivateType type) {
        return craftsNet.getBuilder().isWebSocketServer(type);
    }

    /**
     * Sends a message to all connected WebSocket clients.
     *
     * @param data The message to be sent.
     */
    public void broadcast(String data) {
        connected.forEach((useless, clients) -> clients.forEach(client -> client.sendMessage(data)));
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
        connected.entrySet().parallelStream()
                .filter(entry -> entry.getValue().contains(client))
                .toList()
                .forEach(entry -> {
                    try {
                        entry.getValue().removeIf(client::equals);
                        Thread thread = client.disconnect();
                        thread.interrupt();
                        thread.join(500);
                        if (entry.getValue().isEmpty()) connected.remove(entry.getKey());
                    } catch (InterruptedException ignored) {
                    }
                });
    }

}