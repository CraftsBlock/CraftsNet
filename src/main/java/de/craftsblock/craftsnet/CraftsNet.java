package de.craftsblock.craftsnet;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftscore.utils.ArgumentParser;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.http.builtin.DefaultRoute;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.command.CommandRegistry;
import de.craftsblock.craftsnet.command.commands.PluginCommand;
import de.craftsblock.craftsnet.command.commands.ShutdownCommand;
import de.craftsblock.craftsnet.command.commands.VersionCommand;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;
import de.craftsblock.craftsnet.listeners.ConsoleListener;
import de.craftsblock.craftsnet.logging.FileLogger;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

/**
 * CraftsNet class represents the core component of the CraftsNet framework,
 * providing functionalities for managing various aspects of the system.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 3.0.0
 * @since 1.0.0
 */
public class CraftsNet {

    // Global variables
    public static final String version = "3.0.3-SNAPSHOT";
    private static CraftsNet instance;

    // Lokal instance
    private Builder builder;
    private Logger logger;
    private Thread consoleListener;
    private Thread shutdownThread;

    // Manager instances
    private AddonManager addonManager;
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;
    private RouteRegistry routeRegistry;
    private ServiceManager serviceManager;

    // Server instances
    private WebServer webServer;
    private WebSocketServer webSocketServer;

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

        CraftsNet.create()
                .withWebServer(port)
                .withWebSocketServer(socketport)
                .withSSL(ssl)
                .withDebug(debug)
                .build();
    }

    /**
     * Starts the CraftsNet framework with the provided builder configuration.
     *
     * @param builder The builder instance containing the configuration for starting CraftsNet.
     * @throws IOException If an I/O error occurs during the startup process.
     */
    protected void start(Builder builder) throws IOException {
        // Check if the builder was set and throw an exception if it is already set
        if (this.builder != null && instance == null)
            throw new RuntimeException("");
        // Save the builder and the current instance
        CraftsNet.instance = this;
        this.builder = builder;

        // Start measuring the startup time
        long start = System.currentTimeMillis();

        // Create and initialize the logger & file logger
        logger = new Logger(builder.isDebug());
        FileLogger.start();

        // Setup default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            long identifier = FileLogger.createErrorLog(e);
            logger.error(e, "Exception: " + identifier);
        });

        // Log startup message
        logger.info("CraftsNet v" + version + " boots up");
        Runtime.Version version = Runtime.version();
        logger.debug("JVM Version: " + version.toString() + "; Max recognizable class file version: " + (version.feature() + 44) + "." + version.interim());

        // Initialize listener and route registries, and addon manager
        logger.debug("Initialization of system variables");
        listenerRegistry = new ListenerRegistry();
        routeRegistry = new RouteRegistry();
        commandRegistry = new CommandRegistry();
        serviceManager = new ServiceManager();
        if (!builder.isAddonSystem(ActivateType.DISABLED)) addonManager = new AddonManager();

        // Check if http routes are registered and start the web server if needed
        webServer = new WebServer(builder.getWebServerPort(), builder.isSSL());
        if (builder.isWebServer(ActivateType.ENABLED) || (builder.isWebServer(ActivateType.DYNAMIC))) {
            // Register a default route if nothing has been registered.
            if (!routeRegistry.hasRoutes() && !routeRegistry.hasWebsockets())
                routeRegistry().register(new DefaultRoute());

            // Start the webserver if needed
            if (routeRegistry.hasRoutes()) {
                logger.debug("Setting up the web server");
                webServer.start();
            }
        } else if (builder.isWebServer(ActivateType.DISABLED) && routeRegistry.hasRoutes())
            logger.warning("The web server is forcible disabled, but has registered routes!");

        // Check if webSocket routes are registered and start the websocket server if needed
        webSocketServer = new WebSocketServer(builder.getWebSocketServerPort(), builder.isSSL());
        if (builder.isWebSocketServer(ActivateType.ENABLED) || (builder.isWebSocketServer(ActivateType.DYNAMIC) && routeRegistry.hasWebsockets())) {
            logger.debug("Starting the websocket server");
            webSocketServer.start();
        } else if (builder.isWebSocketServer(ActivateType.DISABLED) && routeRegistry.hasWebsockets())
            logger.warning("The websocket server is forcible disabled, but has registered endpoints!");

        // Register build in commands / listeners
        if (!builder.isCommandSystem(ActivateType.DISABLED)) {
            listenerRegistry.register(new ConsoleListener());
            commandRegistry.getCommand("pl").setExecutor(new PluginCommand());
            commandRegistry.getCommand("pl").addAlias("plugin", "plugins", "addons");
            commandRegistry.getCommand("shutdown").setExecutor(new ShutdownCommand());
            commandRegistry.getCommand("shutdown").addAlias("quit", "exit");
            commandRegistry.getCommand("ver").setExecutor(new VersionCommand());
            commandRegistry.getCommand("ver").addAlias("version", "v");

            // Set up and start the console listener
            this.consoleListener = getConsoleReader();
            logger.debug("Console listener is started");
        }

        // Register a shutdown hook for calling the stop method
        this.shutdownThread = new Thread(this::stop);
        Runtime.getRuntime().addShutdownHook(this.shutdownThread);
        logger.debug("JVM Shutdown Hook is implemented");

        // Log successful startup message with elapsed time
        logger.info("Backend was successfully started after " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Stops the CraftsNet framework.
     */
    public void stop() {
        if (this.consoleListener != null && !this.consoleListener.isInterrupted()) {
            this.consoleListener.interrupt();
            this.consoleListener = null;
        }

        if (this.webServer != null && this.webServer.isRunning()) {
            this.webServer.stop();
            this.webServer = null;
        }

        if (this.webSocketServer != null && this.webSocketServer.isRunning()) {
            this.webSocketServer.stop();
            this.webSocketServer = null;
        }

        if (this.addonManager != null) {
            this.addonManager.stop();
            this.addonManager = null;
        }

        try {
            if (this.shutdownThread != null) {
                Runtime.getRuntime().removeShutdownHook(this.shutdownThread);
                this.shutdownThread = null;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    /**
     * Retrieves the console reader thread used for input.
     *
     * @return The console reader thread.
     */
    @NotNull
    private Thread getConsoleReader() {
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

    /**
     * Retrieves the addon manager instance for managing addons.
     *
     * @return The addon manager instance.
     */
    public AddonManager addonManager() {
        return addonManager;
    }

    /**
     * Retrieves the command registry instance for managing commands.
     *
     * @return The command registry instance.
     */
    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    /**
     * Retrieves the listener registry instance for managing event listeners.
     *
     * @return The listener registry instance.
     */
    public ListenerRegistry listenerRegistry() {
        return listenerRegistry;
    }

    /**
     * Retrieves the route registry instance for managing web and web socket routes.
     *
     * @return The route registry instance.
     */
    public RouteRegistry routeRegistry() {
        return routeRegistry;
    }

    /**
     * Retrieves the service manager instance for managing services.
     *
     * @return The service manager instance.
     */
    public ServiceManager serviceManager() {
        return serviceManager;
    }

    /**
     * Retrieves the web server instance for handling HTTP requests.
     *
     * @return The web server instance.
     */
    public WebServer webServer() {
        return webServer;
    }

    /**
     * Retrieves the WebSocket server instance for handling WebSocket connections.
     *
     * @return The WebSocket server instance.
     */
    public WebSocketServer webSocketServer() {
        return webSocketServer;
    }

    /**
     * Retrieves the logger instance for logging messages.
     *
     * @return The logger instance.
     */
    public Logger logger() {
        return logger;
    }

    /**
     * Retrieves the builder instance used for configuring CraftsNet.
     *
     * @return The builder instance.
     */
    public Builder getBuilder() {
        return builder;
    }

    /**
     * Retrieves the current active instance of CraftsNet.
     *
     * @return The singleton instance of CraftsNet.
     */
    public static CraftsNet instance() {
        return instance;
    }

    /**
     * Creates a new builder instance for configuring CraftsNet.
     *
     * @return A new builder instance.
     */
    public static CraftsNet.Builder create() {
        return new Builder();
    }

    /**
     * Builder class for configuring the CraftsNet framework.
     *
     * @author CraftsBlock
     * @author Philipp Maywald
     * @version 1.0.0
     * @since 3.0.3
     */
    public final static class Builder {

        private int webServerPort;
        private int webSocketServerPort;

        private ActivateType webServer;
        private ActivateType webSocketServer;
        private ActivateType addonSystem;
        private ActivateType commandSystem;

        private boolean debug;
        private boolean ssl;

        /**
         * Constructs a new Builder instance with default configuration settings.
         */
        public Builder() {
            webServerPort = 5000;
            webSocketServerPort = 5001;
            webServer = webSocketServer = ActivateType.DYNAMIC;
            addonSystem = commandSystem = ActivateType.ENABLED;
            debug = false;
            ssl = false;
        }

        /**
         * Specifies the port for the web server.
         *
         * @param port The port number for the web server.
         * @return The Builder instance.
         */
        public Builder withWebServer(int port) {
            return withWebServer(ActivateType.DYNAMIC, port);
        }

        /**
         * Specifies the activation type and port for the web server.
         *
         * @param type The activation type for the web server.
         * @param port The port number for the web server.
         * @return The Builder instance.
         */
        public Builder withWebServer(ActivateType type, int port) {
            this.webServer = type;
            this.webServerPort = port;
            return this;
        }

        /**
         * Specifies the port for the WebSocket server.
         *
         * @param port The port number for the WebSocket server.
         * @return The Builder instance.
         */
        public Builder withWebSocketServer(int port) {
            return withWebSocketServer(ActivateType.DYNAMIC, port);
        }

        /**
         * Specifies the activation type and port for the WebSocket server.
         *
         * @param type The activation type for the WebSocket server.
         * @param port The port number for the WebSocket server.
         * @return The Builder instance.
         */
        public Builder withWebSocketServer(ActivateType type, int port) {
            this.webSocketServer = type;
            this.webSocketServerPort = port;
            return this;
        }

        /**
         * Specifies the activation type for the addon system.
         *
         * @param type The activation type for the addon system.
         * @return The Builder instance.
         */
        public Builder withAddonSystem(ActivateType type) {
            this.addonSystem = type;
            return this;
        }

        /**
         * Specifies the activation type for the command system.
         *
         * @param type The activation type for the command system.
         * @return The Builder instance.
         */
        public Builder withCommandSystem(ActivateType type) {
            this.commandSystem = type;
            return this;
        }

        /**
         * Specifies whether debug mode should be enabled.
         *
         * @param enabled true if debug mode should be enabled, false otherwise.
         * @return The Builder instance.
         */
        public Builder withDebug(boolean enabled) {
            this.debug = enabled;
            return this;
        }

        /**
         * Specifies whether SSL should be enabled.
         *
         * @param enabled true if SSL should be enabled, false otherwise.
         * @return The Builder instance.
         */
        public Builder withSSL(boolean enabled) {
            ssl = enabled;
            return this;
        }

        /**
         * Retrieves the port number configured for the web server.
         *
         * @return The port number for the web server.
         */
        public int getWebServerPort() {
            return webServerPort;
        }

        /**
         * Retrieves the activation type configured for the web server.
         *
         * @return The activation type for the web server.
         */
        public ActivateType getWebServer() {
            return webServer;
        }

        /**
         * Checks if the web server is configured with the specified activation type.
         *
         * @param type The activation type to check.
         * @return true if the web server is configured with the specified activation type, false otherwise.
         */
        public boolean isWebServer(ActivateType type) {
            return webServer == type;
        }

        /**
         * Retrieves the port number configured for the WebSocket server.
         *
         * @return The port number for the WebSocket server.
         */
        public int getWebSocketServerPort() {
            return webSocketServerPort;
        }

        /**
         * Retrieves the activation type configured for the WebSocket server.
         *
         * @return The activation type for the WebSocket server.
         */
        public ActivateType getWebSocketServer() {
            return webSocketServer;
        }

        /**
         * Checks if the WebSocket server is configured with the specified activation type.
         *
         * @param type The activation type to check.
         * @return true if the WebSocket server is configured with the specified activation type, false otherwise.
         */
        public boolean isWebSocketServer(ActivateType type) {
            return webSocketServer == type;
        }

        /**
         * Retrieves the activation type configured for the addon system.
         *
         * @return The activation type for the addon system.
         */
        public ActivateType getAddonSystem() {
            return addonSystem;
        }

        /**
         * Checks if the addon system is configured with the specified activation type.
         *
         * @param type The activation type to check.
         * @return true if the addon system is configured with the specified activation type, false otherwise.
         */
        public boolean isAddonSystem(ActivateType type) {
            return addonSystem == type;
        }

        /**
         * Retrieves the activation type configured for the command system.
         *
         * @return The activation type for the command system.
         */
        public ActivateType getCommandSystem() {
            return commandSystem;
        }

        /**
         * Checks if the command system is configured with the specified activation type.
         *
         * @param type The activation type to check.
         * @return true if the command system is configured with the specified activation type, false otherwise.
         */
        public boolean isCommandSystem(ActivateType type) {
            return commandSystem == type;
        }

        /**
         * Checks if debug mode is enabled.
         *
         * @return true if debug mode is enabled, false otherwise.
         */
        public boolean isDebug() {
            return debug;
        }

        /**
         * Checks if SSL is enabled.
         *
         * @return true if SSL is enabled, false otherwise.
         */
        public boolean isSSL() {
            return ssl;
        }

        /**
         * Builds and starts the CraftsNet framework with the configured settings.
         *
         * @return The constructed CraftsNet instance.
         * @throws IOException If an I/O error occurs during the startup process.
         */
        public CraftsNet build() throws IOException {
            CraftsNet craftsNet = new CraftsNet();
            craftsNet.start(this);
            return craftsNet;
        }

    }

    /**
     * Enum representing activation types for various components in the CraftsNet framework.
     *
     * @author CraftsBlock
     * @author Philipp Maywald
     * @version 1.0.0
     * @since 3.0.3
     */
    public enum ActivateType {

        /**
         * Indicates that the component is enabled.
         */
        ENABLED,

        /**
         * Indicates that the component is disabled.
         */
        DISABLED,

        /**
         * Indicates that the activation of the component is dynamic, possibly determined at runtime.
         */
        DYNAMIC

    }

}
