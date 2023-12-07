package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.utils.Logger;
import de.craftsblock.craftsnet.utils.SSL;

import javax.net.ssl.SSLServerSocketFactory;
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

/**
 * The WebSocketServer class represents a simple WebSocket server implementation. It allows WebSocket clients to connect,
 * manages their connections, and enables sending messages to connected clients.
 * The server can be configured to use SSL encryption by providing the necessary SSL key file.
 * It uses a ServerSocket to listen for incoming connections on the specified port.
 *
 * @author CraftsBlock
 * @version 1.1
 * @see WebSocketClient
 * @since 2.1.1
 */
public class WebSocketServer {

    private final Logger logger = CraftsNet.logger;
    private Thread connector;
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketClient>> connected;
    private final ConcurrentLinkedQueue<Thread> clients = new ConcurrentLinkedQueue<>();
    private ServerSocket serverSocket;
    private boolean running;

    /**
     * Constructs a WebSocketServer instance with the specified port number.
     *
     * @param port    The port number on which the server will listen for incoming connections.
     * @param ssl     A boolean flag indicating whether SSL encryption should be used (true for HTTPS, false for HTTP).
     * @param ssl_key The key which is used to secure the private key while running (applicable only when ssl is true).
     */
    public WebSocketServer(int port, boolean ssl, String ssl_key) {
        this(port, 0, ssl, ssl_key);
    }

    /**
     * Constructs a WebSocketServer instance with the specified port number and backlog size.
     *
     * @param port    The port number on which the server will listen for incoming connections.
     * @param backlog The size of the backlog for the server socket.
     * @param ssl     A boolean flag indicating whether SSL encryption should be used.
     * @param ssl_key The key which is used to secure the private key while running.
     */
    public WebSocketServer(int port, int backlog, boolean ssl, String ssl_key) {
        try {
            logger.info("Websocket server will be started on port " + port);
            if (!ssl) serverSocket = new ServerSocket(port, backlog);
            else {
                SSLServerSocketFactory sslServerSocketFactory = SSL.load("./certificates/fullchain.pem", "./certificates/privkey.pem", ssl_key)
                        .getServerSocketFactory();
                serverSocket = sslServerSocketFactory.createServerSocket(port, backlog);
            }
            connected = new ConcurrentHashMap<>();
            running = true;
        } catch (IOException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException |
                 KeyStoreException | CertificateException e) {
            logger.error(e);
        }
    }

    /**
     * Starts the WebSocket server and waits for incoming connections.
     */
    public void start() {
        connector = new Thread(() -> {
            int i = 0;
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    Socket socket = serverSocket.accept();
                    Thread client = new Thread(new WebSocketClient(socket, this));
                    client.setName("Websocket#" + i++);
                    client.start();
                    clients.add(client);
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        });
        connector.setName("Websocket Server - Connector");
        connector.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        logger.debug("Websocket Server JVM Shutdown Hook is initialized");
    }

    /**
     * Stops the WebSocket server and closes all connections.
     */
    public void stop() {
        running = false;
        try {
            connected.forEach((useless, client) -> client.forEach(WebSocketClient::disconnect));
            clients.forEach(Thread::interrupt);
            connected.clear();
            clients.clear();
            serverSocket.close();
            if (connector != null)
                connector.interrupt();
        } catch (IOException e) {
            logger.error(e);
        }
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