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

public class WebSocketServer {

    private ConcurrentLinkedQueue<WebSocketClient> clients;
    private ConcurrentHashMap<RouteRegistry.SocketMapping, ConcurrentLinkedQueue<WebSocketClient>> connected;
    private ServerSocket serverSocket;
    private boolean running;

    public WebSocketServer(int port, boolean ssl, String ssl_key) {
        this(port, 0, ssl, ssl_key);
    }

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

    public void broadcast(String data) {
        clients.forEach(client -> client.sendMessage(data));
    }

    public void broadcast(RouteRegistry.SocketMapping mapping, String data) {
        if (connected.containsKey(mapping)) connected.get(mapping).forEach(client -> client.sendMessage(data));
    }

    protected void add(RouteRegistry.SocketMapping mapping, WebSocketClient client) {
        if (!connected.containsKey(mapping)) connected.put(mapping, new ConcurrentLinkedQueue<>());
        connected.get(mapping).add(client);
    }

    protected void remove(WebSocketClient client) {
        clients.remove(client);
        connected.values().stream()
                .filter(webSocketClients -> webSocketClients.contains(client))
                .toList()
                .forEach(list -> list.removeIf(client::equals));
    }

}