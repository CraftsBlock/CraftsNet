package de.craftsblock.craftsnet;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftscore.utils.ArgumentParser;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.http.body.BodyRegistry;
import de.craftsblock.craftsnet.api.http.builtin.DefaultRoute;
import de.craftsblock.craftsnet.api.websocket.DefaultPingResponder;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtensionRegistry;
import de.craftsblock.craftsnet.command.CommandRegistry;
import de.craftsblock.craftsnet.command.commands.PluginCommand;
import de.craftsblock.craftsnet.command.commands.ReloadCommand;
import de.craftsblock.craftsnet.command.commands.ShutdownCommand;
import de.craftsblock.craftsnet.command.commands.VersionCommand;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;
import de.craftsblock.craftsnet.listeners.ConsoleListener;
import de.craftsblock.craftsnet.logging.FileLogger;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.logging.LoggerImpl;
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
 * @version 3.0.1
 * @since 1.0.0-SNAPSHOT
 */
public class CraftsNet {

    // Global variables
    public static final String version = "3.0.7-SNAPSHOT";

    // Lokal instance
    private Builder builder;
    private Logger logger;
    private FileLogger fileLogger;
    private Thread consoleListener;
    private Thread shutdownThread;

    // Manager instances
    private AddonManager addonManager;
    private BodyRegistry bodyRegistry;
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;
    private RouteRegistry routeRegistry;
    private ServiceManager serviceManager;
    private WebSocketExtensionRegistry webSocketExtensionRegistry;

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
        // Check if the builder was set or CraftsNet is already running and throw an exception if needed.
        if (this.builder != null)
            throw new RuntimeException("The instance of CraftsNet has already been started!");
        // Save the builder and the current instance
        this.builder = builder;

        // Start measuring the startup time
        long start = System.currentTimeMillis();

        // Create and initialize the logger & file logger
        logger = builder.getCustomLogger();

        // Checks if the file logger was enabled and start it if so
        if (builder.isFileLogger(ActivateType.ENABLED)) {
            fileLogger = new FileLogger();
            fileLogger.start();
        }

        // Setup default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (fileLogger != null) {
                long identifier = fileLogger.createErrorLog(this, e);
                logger.error(e, "Throwable: " + identifier);
                return;
            }

            logger.error(e);
        });

        // Log startup message
        logger.info("CraftsNet v" + version + " boots up");
        Runtime.Version version = Runtime.version();
        logger.debug("JVM Version: " + version.toString() + "; Max recognizable class file version: " + (version.feature() + 44) + "." + version.interim());

        // Initialize listener and route registries, and addon manager
        logger.debug("Initialization of system variables");
        listenerRegistry = new ListenerRegistry();
        routeRegistry = new RouteRegistry(this);
        commandRegistry = new CommandRegistry(this);
        serviceManager = new ServiceManager();
        if (!builder.isAddonSystem(ActivateType.DISABLED)) addonManager = new AddonManager(this);

        // Check if http routes are registered and start the web server if needed
        if (builder.isWebServer(ActivateType.ENABLED) || builder.isWebServer(ActivateType.DYNAMIC)) {
            logger.info("Setting up the web server");
            webServer = new WebServer(this, builder.getWebServerPort(), builder.isSSL());

            // Set the bodyRegistry if the webserver is enabled.
            bodyRegistry = new BodyRegistry();

            // Register a default route if nothing has been registered.
            if (!routeRegistry.hasRoutes() && !routeRegistry.hasWebsockets())
                routeRegistry().register((Handler) new DefaultRoute());

            // Start the webserver if needed
            if (routeRegistry.hasRoutes() || builder.isWebServer(ActivateType.ENABLED)) {
                webServer.start();
            }
        } else if (builder.isWebServer(ActivateType.DISABLED) && routeRegistry.hasRoutes())
            logger.warning("The web server is forcible disabled, but has registered routes!");

        // Check if webSocket routes are registered and start the websocket server if needed
        if (builder.isWebSocketServer(ActivateType.ENABLED) || builder.isWebSocketServer(ActivateType.DYNAMIC)) {
            logger.info("Setting up the websocket server");
            webSocketExtensionRegistry = new WebSocketExtensionRegistry();
            webSocketServer = new WebSocketServer(this, builder.getWebSocketServerPort(), builder.isSSL());
            DefaultPingResponder.register(this);

            if (routeRegistry.hasWebsockets() || builder.isWebSocketServer(ActivateType.ENABLED)) {
                webSocketServer.start();
            }
        } else if (builder.isWebSocketServer(ActivateType.DISABLED) && routeRegistry.hasWebsockets())
            logger.warning("The websocket server is forcible disabled, but has registered endpoints!");

        // Register build in commands / listeners
        if (!builder.isCommandSystem(ActivateType.DISABLED)) {
            listenerRegistry.register(new ConsoleListener(this));
            commandRegistry.getCommand("pl").setExecutor(new PluginCommand(this));
            commandRegistry.getCommand("pl").addAlias("plugin", "plugins", "addons");
            commandRegistry.getCommand("restart").setExecutor(new ReloadCommand(this));
            commandRegistry.getCommand("restart").addAlias("reload", "rl");
            commandRegistry.getCommand("shutdown").setExecutor(new ShutdownCommand(this));
            commandRegistry.getCommand("shutdown").addAlias("quit", "exit", "stop");
            commandRegistry.getCommand("ver").setExecutor(new VersionCommand());
            commandRegistry.getCommand("ver").addAlias("version", "v");

            // Set up and start the console listener
            this.consoleListener = getConsoleReader();
            logger.debug("Console listener is started");
        }

        // Register a shutdown hook for calling the stop method
        this.shutdownThread = new Thread(this::stop, "CraftsNet Shutdown");
        Runtime.getRuntime().addShutdownHook(this.shutdownThread);
        logger.debug("JVM Shutdown Hook is implemented");

        // Log successful startup message with elapsed time
        logger.info("Backend was successfully started after " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Stops the CraftsNet framework.
     */
    public void stop() {
        logger.info("Shutdown request has been received");
        if (this.consoleListener != null && !this.consoleListener.isInterrupted()) {
            logger.info("Closing the console input listener");
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

        if (this.fileLogger != null) {
            logger.info("Disconnecting the file logger");
            this.fileLogger.stop();
            this.fileLogger = null;
        }

        try {
            if (this.shutdownThread != null) {
                logger.info("Removing jvm shutdown hook");
                Runtime.getRuntime().removeShutdownHook(this.shutdownThread);
                this.shutdownThread = null;
            }
        } catch (IllegalStateException ignored) {
        }

        logger.info("CraftsNet has been shutdown");
    }

    /**
     * Restarts the CraftsNet framework.
     */
    public void restart() throws IOException {
        restart(null);
    }

    /**
     * Restarts the CraftsNet framework.
     */
    public void restart(Runnable executeBetween) {
        Builder builder = this.builder;
        this.builder = null;
        new Thread(() -> {
            stop();
            if (executeBetween != null) executeBetween.run();
            try {
                start(builder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "main").start();
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
     * Retrieves the body registry instance for manging body types.
     *
     * @return The body registry instance.
     * @since CraftsNet-3.0.4
     */
    public BodyRegistry bodyRegistry() {
        return bodyRegistry;
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
     * Retrieves the extension registry for the websocket protocol.
     *
     * @return The websocket extension registry.
     */
    public WebSocketExtensionRegistry webSocketExtensionRegistry() {
        return webSocketExtensionRegistry;
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
     * Retrieves the file logger instance for logging messages to an file.
     *
     * @return The file logger instance.
     */
    public FileLogger fileLogger() {
        return fileLogger;
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

        private ActivateType fileLogger;
        private Logger logger;

        private boolean debug;
        private boolean ssl;

        /**
         * Constructs a new Builder instance with default configuration settings.
         */
        public Builder() {
            webServerPort = 5000;
            webSocketServerPort = 5001;
            webServer = webSocketServer = ActivateType.DYNAMIC;
            addonSystem = commandSystem = fileLogger = ActivateType.ENABLED;
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
         * Specifies the port for the web server.
         *
         * @param type The activation type for the web server.
         * @return The Builder instance.
         * @since 3.0.5
         */
        public Builder withWebServer(ActivateType type) {
            return withWebServer(type, this.webServerPort);
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
         * @return The Builder instance.
         * @since 3.0.5
         */
        public Builder withWebSocketServer(ActivateType type) {
            return withWebSocketServer(type, this.webSocketServerPort);
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
         * Specifies the activation type for the file logger.
         *
         * @param type The activation type for the file logger.
         * @return The Builder instance.
         * @since 3.0.5
         */
        public Builder withFileLogger(ActivateType type) {
            this.fileLogger = type;
            return this;
        }

        /**
         * Sets a custom logger which will be used by CraftsNet. It can be null when the default logger should be used.
         *
         * @param logger The instance of the custom logger.
         * @return The Builder instance.
         * @since 3.0.5
         */
        public Builder withCustomLogger(Logger logger) {
            this.logger = logger;
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
         * Checks if the file logger is configured with the specified activation type.
         *
         * @param type The activation type to check.
         * @return true if the file logger is configured with the specified activation type, false otherwise.
         * @since 3.0.5
         */
        public boolean isFileLogger(ActivateType type) {
            return fileLogger == type;
        }

        /**
         * Retrieves the custom logger which should be used by CraftsNet.
         *
         * @return The logger instance.
         * @since 3.0.5
         */
        public Logger getCustomLogger() {
            return logger;
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
            // Set up the logger if no logger was given
            if (logger == null) logger = new LoggerImpl(isDebug());

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
