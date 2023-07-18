package de.craftsblock.craftsnet;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftscore.utils.ArgumentParser;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;
import de.craftsblock.craftsnet.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

public class Main {

    public static AddonManager addonManager;
    public static ListenerRegistry listenerRegistry;

    public static WebServer webServer;
    public static WebSocketServer webSocketServer;
    public static RouteRegistry routeRegistry;

    public static Logger logger;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        long start = System.currentTimeMillis();
        ArgumentParser parser = new ArgumentParser(args);
        boolean debug = parser.isPresent("debug");
        boolean ssl = parser.isPresent("ssl");
        String ssl_key = (ssl ? parser.getAsString("ssl") : null);
        int port = (parser.isPresent("http-port") ? parser.getAsInt("http-port") : 5000);
        int socketport = (parser.isPresent("socket-port") ? parser.getAsInt("socket-port") : 5001);

        logger = new Logger(debug);

        logger.info("Backend wird gestartet");
        logger.debug("Initialisierung von System Variablen");
        listenerRegistry = new ListenerRegistry();
        routeRegistry = new RouteRegistry();
        addonManager = new AddonManager();

        logger.debug("Aufsetzen des Web Servers");
        webServer = new WebServer(port, ssl, ssl_key);

        if (routeRegistry.hasWebsockets()) {
            logger.debug("Starten des Websocket Servers");
            webSocketServer = new WebSocketServer(socketport, ssl, ssl_key);
            webSocketServer.start();
        }

        logger.debug("Konsolen Listener wird gestartet");
        Thread console = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if ((line = reader.readLine()) != null)
                        listenerRegistry.call(new ConsoleMessageEvent(line));
                }
            } catch (IOException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }, "Console Reader");
        console.start();
        logger.debug("Console Listener JVM Shutdown Hook wird implementiert");
        Runtime.getRuntime().addShutdownHook(new Thread(console::interrupt));
        logger.info("Backend wurde erfolgreich nach " + (System.currentTimeMillis() - start) + "ms gestartet");
    }

}
