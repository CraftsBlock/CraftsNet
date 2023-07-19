package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.Main;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.utils.Logger;
import de.craftsblock.craftsnet.utils.SSL;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
 * @see WebSocketClient
 * @since 2.1.1
 */
public class WebSocketServer {

    private ConcurrentLinkedQueue<WebSocketClient> clients;
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketClient>> connected;
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
        Logger logger = Main.logger;
        try {
            logger.info("Websocket Server wird auf Port " + port + " gestartet");
            if (!ssl) serverSocket = new ServerSocket(port, backlog);
            else {
                SSLServerSocketFactory sslServerSocketFactory = SSL.load("./certificates/fullchain.pem", "./certificates/privkey.pem", ssl_key)
                        .getServerSocketFactory();
                serverSocket = sslServerSocketFactory.createServerSocket(port, backlog);
            }
            clients = new ConcurrentLinkedQueue<>();
            connected = new ConcurrentHashMap<>();
            running = true;
        } catch (IOException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException |
                 KeyStoreException | CertificateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the WebSocket server and waits for incoming connections.
     */
    public void start() {
        Thread connector = new Thread(() -> {
            int i = 0;
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    Socket socket = serverSocket.accept();
                    WebSocketClient client = new WebSocketClient(socket, this);
                    clients.add(client);
                    client.setName("Websocket#" + i++);
                    client.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        connector.setName("Websocket Server - Connector");
        connector.start();
        Main.logger.debug("Websocket Server JVM Shutdown Hook wird initialisiert");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            connector.interrupt();
            clients.forEach(WebSocketClient::interrupt);
        }));
    }

    /**
     * Stops the WebSocket server and closes all connections.
     */
    public void stop() {
        running = false;
        try {
            clients.forEach(WebSocketClient::disconnect);
            clients.clear();
            connected.forEach((useless, client) -> client.forEach(WebSocketClient::disconnect));
            connected.clear();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to all connected WebSocket clients.
     *
     * @param data The message to be sent.
     */
    public void broadcast(String data) {
        clients.forEach(client -> client.sendMessage(data));
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
        if (!connected.containsKey(path)) connected.put(path, new ConcurrentLinkedQueue<>());
        connected.get(path).add(client);
    }

    /**
     * Removes a WebSocket client from the server and the associated path.
     *
     * @param client The WebSocket client that will be removed.
     */
    protected void remove(WebSocketClient client) {
        clients.remove(client);
        connected.values().stream()
                .filter(webSocketClients -> webSocketClients.contains(client))
                .toList()
                .forEach(list -> list.removeIf(client::equals));
    }

}