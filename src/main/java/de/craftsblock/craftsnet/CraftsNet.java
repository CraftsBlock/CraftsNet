package de.craftsblock.craftsnet;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftscore.utils.ArgumentParser;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.command.CommandRegistry;
import de.craftsblock.craftsnet.command.commands.ShutdownCommand;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;
import de.craftsblock.craftsnet.listeners.ConsoleListener;
import de.craftsblock.craftsnet.utils.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

/**
 * The Main class is the entry point of the application. It initializes and starts the backend components, such as
 * the HTTP and WebSocket servers, as well as manages addons and console listeners.
 *
 * @author CraftsBlock
 * @since 1.0.0
 */
public class CraftsNet {

    // Manager instances
    public static AddonManager addonManager;
    public static CommandRegistry commandRegistry;
    public static ListenerRegistry listenerRegistry;
    public static RouteRegistry routeRegistry;

    // Server instances
    public static WebServer webServer;
    public static WebSocketServer webSocketServer;

    // Logger instance
    public static Logger logger;

    /**
     * The main method is the entry point of the application. It parses command-line arguments, initializes the
     * backend components, and starts the servers.
     *
     * @param args The command-line arguments passed to the application.
     * @throws IOException            If an I/O error occurs while starting the servers.
     * @throws ClassNotFoundException If a class is not found during the initialization of addons.
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        long start = System.currentTimeMillis(); // Start measuring the startup time

        // Parse command-line arguments
        ArgumentParser parser = new ArgumentParser(args);
        boolean debug = parser.isPresent("debug");
        boolean ssl = parser.isPresent("ssl");
        String ssl_key = (ssl ? parser.getAsString("ssl") : null);
        int port = (parser.isPresent("http-port") ? parser.getAsInt("http-port") : 5000);
        int socketport = (parser.isPresent("socket-port") ? parser.getAsInt("socket-port") : 5001);

        logger = new Logger(debug); // Create and initialize the logger
        logger.info("Backend wird gestartet"); // Log startup message

        // Initialize listener and route registries, and addon manager
        logger.debug("Initialisierung von System Variablen");
        listenerRegistry = new ListenerRegistry();
        routeRegistry = new RouteRegistry();
        commandRegistry = new CommandRegistry();
        addonManager = new AddonManager();

        // Check if http routes are registered and start the web server if needed
        if (routeRegistry.hasRoutes()) {
            logger.debug("Aufsetzen des Web Servers");
            webServer = new WebServer(port, ssl, ssl_key);
        }

        // Check if webSocket routes are registered and start the websocket server if needed
        if (routeRegistry.hasWebsockets()) {
            logger.debug("Starten des Websocket Servers");
            webSocketServer = new WebSocketServer(socketport, ssl, ssl_key);
            webSocketServer.start();
        }

        // Register build in commands / listeners
        listenerRegistry.register(new ConsoleListener());
        commandRegistry.getCommand("shutdown").setExecutor(new ShutdownCommand());
        commandRegistry.getCommand("quit").setExecutor(new ShutdownCommand());

        // Set up and start the console listener
        logger.debug("Konsolen Listener wird gestartet");
        Thread console = getConsoleReader();

        // Register a shutdown hook for the console listener
        logger.debug("Console Listener JVM Shutdown Hook wird implementiert");
        Runtime.getRuntime().addShutdownHook(new Thread(console::interrupt));

        logger.info("Backend wurde erfolgreich nach " + (System.currentTimeMillis() - start) + "ms gestartet"); // Log successful startup message with elapsed time
    }

    @NotNull
    private static Thread getConsoleReader() {
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
        return console;
    }

}