package de.craftsblock.craftsnet;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftscore.utils.ArgumentParser;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.command.CommandRegistry;
import de.craftsblock.craftsnet.command.commands.PluginCommand;
import de.craftsblock.craftsnet.command.commands.ShutdownCommand;
import de.craftsblock.craftsnet.command.commands.VersionCommand;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;
import de.craftsblock.craftsnet.listeners.ConsoleListener;
import de.craftsblock.craftsnet.utils.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

/**
 * The Main class is the entry point of the application. It initializes and starts the backend components, such as
 * the HTTP and WebSocket servers, as well as manages addons and console listeners.
 *
 * @author CraftsBlock
 * @version 2.0
 * @since 1.0.0
 */
public class CraftsNet {

    // Information variables
    public static final String version = "3.0.0-SNAPSHOT";

    // Manager instances
    private static AddonManager addonManager;
    private static CommandRegistry commandRegistry;
    private static ListenerRegistry listenerRegistry;
    private static RouteRegistry routeRegistry;
    private static ServiceManager serviceManager;

    // Server instances
    private static WebServer webServer;
    private static WebSocketServer webSocketServer;

    // Logger instance
    private static Logger logger;

    /**
     * The main method is the entry point of the application. It parses command-line arguments, initializes the
     * backend
     *
     * @param args The command-line arguments passed to the application.
     * @throws IOException If an I/O error occurs while starting the servers.
     */
    public static void main(String[] args) throws IOException {
        // Parse command-line arguments
        ArgumentParser parser = new ArgumentParser(args);
        boolean debug = parser.isPresent("debug");
        boolean ssl = parser.isPresent("ssl");
        int port = (parser.isPresent("http-port") ? parser.getAsInt("http-port") : 5000);
        int socketport = (parser.isPresent("socket-port") ? parser.getAsInt("socket-port") : 5001);

        start(debug, ssl, port, socketport, false, false);
    }

    /**
     * This method starts the backend service.
     *
     * @param debug          Set to true for debug mode.
     * @param ssl            Set to true to enable SSL.
     * @param port           The HTTP port to listen on.
     * @param socketport     The WebSocket port to listen on.
     * @param forceHttp      Set to true to force HTTP server start.
     * @param forceWebsocket Set to true to force WebSocket server start.
     * @throws IOException If an I/O error occurs during startup.
     */
    private static void start(boolean debug, boolean ssl, int port, int socketport, boolean forceHttp, boolean forceWebsocket) throws IOException {
        long start = System.currentTimeMillis(); // Start measuring the startup time
        logger = new Logger(debug); // Create and initialize the logger
        // Setup default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (e instanceof Exception exception) logger.error(exception);
        });
        // Log startup message
        logger.info("CraftsNet v" + version + " boots up");

        // Initialize listener and route registries, and addon manager
        logger.debug("Initialization of system variables");
        listenerRegistry = new ListenerRegistry();
        routeRegistry = new RouteRegistry();
        commandRegistry = new CommandRegistry();
        serviceManager = new ServiceManager();
        addonManager = new AddonManager();

        // Check if http routes are registered and start the web server if needed
        if (routeRegistry.hasRoutes() || forceHttp) {
            logger.debug("Setting up the web server");
            webServer = new WebServer(port, ssl);
        }

        // Check if webSocket routes are registered and start the websocket server if needed
        if (routeRegistry.hasWebsockets() || forceWebsocket) {
            logger.debug("Starting the websocket server");
            webSocketServer = new WebSocketServer(socketport, ssl);
            webSocketServer.start();
        }

        // Register build in commands / listeners
        listenerRegistry.register(new ConsoleListener());
        commandRegistry.getCommand("pl").setExecutor(new PluginCommand());
        commandRegistry.getCommand("pl").addAlias("plugin", "plugins", "addons");
        commandRegistry.getCommand("shutdown").setExecutor(new ShutdownCommand());
        commandRegistry.getCommand("shutdown").addAlias("quit", "exit");
        commandRegistry.getCommand("ver").setExecutor(new VersionCommand());
        commandRegistry.getCommand("ver").addAlias("version", "v");

        // Set up and start the console listener
        Thread console = getConsoleReader();
        logger.debug("Console listener is started");

        // Register a shutdown hook for the console listener
        Runtime.getRuntime().addShutdownHook(new Thread(console::interrupt));
        logger.debug("Console Listener JVM Shutdown Hook is implemented");

        // Log successful startup message with elapsed time
        logger.info("Backend was successfully started after " + (System.currentTimeMillis() - start) + "ms");
    }

    @NotNull
    private static Thread getConsoleReader() {
        Thread console = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            try {
                while (!Thread.currentThread().isInterrupted())
                    if ((line = reader.readLine()) != null) listenerRegistry.call(new ConsoleMessageEvent(line));
            } catch (IOException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }, "Console Reader");
        console.start();
        return console;
    }

    public static AddonManager addonManager() {
        return addonManager;
    }

    public static CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    public static ListenerRegistry listenerRegistry() {
        return listenerRegistry;
    }

    public static RouteRegistry routeRegistry() {
        return routeRegistry;
    }

    public static ServiceManager serviceManager() {
        return serviceManager;
    }

    public static WebServer webServer() {
        return webServer;
    }

    public static WebSocketServer webSocketServer() {
        return webSocketServer;
    }

    public static Logger logger() {
        return logger;
    }

}
