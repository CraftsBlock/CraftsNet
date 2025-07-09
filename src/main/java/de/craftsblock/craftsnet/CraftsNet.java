package de.craftsblock.craftsnet;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.http.body.BodyRegistry;
import de.craftsblock.craftsnet.api.http.builtin.DefaultRoute;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoderRegistry;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareRegistry;
import de.craftsblock.craftsnet.api.requirements.RequirementRegistry;
import de.craftsblock.craftsnet.api.session.SessionCache;
import de.craftsblock.craftsnet.api.websocket.DefaultPingResponder;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtensionRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterRegistry;
import de.craftsblock.craftsnet.autoregister.loaders.AutoRegisterLoader;
import de.craftsblock.craftsnet.builder.ActivateType;
import de.craftsblock.craftsnet.builder.AddonContainingBuilder;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.command.CommandRegistry;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;
import de.craftsblock.craftsnet.listeners.ConsoleListener;
import de.craftsblock.craftsnet.logging.mutate.LogStream;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.FileHelper;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import de.craftsblock.craftsnet.utils.versions.Versions;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

/**
 * CraftsNet class represents the core component of the CraftsNet framework,
 * providing functionalities for managing various aspects of the system.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 3.5.0
 * @since 1.0.0-SNAPSHOT
 */
public class CraftsNet {

    // Global variables
    public static final String version = "3.4.4-pre6";

    // Local instance
    private CraftsNetBuilder builder;
    private Logger logger;
    private LogStream logStream;
    private FileHelper fileHelper;
    private Thread consoleListener;
    private BufferedReader consoleReader;

    // Threads
    private Thread shutdownThread;
    private Thread.UncaughtExceptionHandler oldDefaultUncaughtExceptionHandler;

    // Manager instances
    private AddonManager addonManager;
    private AutoRegisterRegistry autoRegisterRegistry;
    private BodyRegistry bodyRegistry;
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;
    private MiddlewareRegistry middlewareRegistry;
    private RequirementRegistry requirementRegistry;
    private RouteRegistry routeRegistry;
    private ServiceManager serviceManager;
    private SessionCache sessionCache;
    private StreamEncoderRegistry streamEncoderRegistry;
    private WebSocketExtensionRegistry webSocketExtensionRegistry;

    // Server instances
    private WebServer webServer;
    private WebSocketServer webSocketServer;

    // Status variables
    private boolean started;

    /**
     * The main method is the entry point of the application. It parses command-line arguments, initializes the
     * backend
     *
     * @param args The command-line arguments passed to the application.
     * @throws IOException If an I/O error occurs while starting the servers.
     */
    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("CraftsNet Main");
        CraftsNet.create().withArgs(args).build();
    }

    /**
     * Starts the CraftsNet framework with the provided builder configuration.
     *
     * @param builder The builder instance containing the configuration for starting CraftsNet.
     * @throws IOException If an I/O error occurs during the startup process.
     */
    public void start(CraftsNetBuilder builder) throws IOException {
        // Check if the builder was set or CraftsNet is already running and throw an exception if needed.
        if (started)
            throw new RuntimeException("The instance of CraftsNet has already been started!");
        // Save the builder and the current instance
        this.builder = builder;

        // Start measuring the startup time
        long start = System.currentTimeMillis();

        // Create and initialize the logger & file logger
        logger = builder.getCustomLogger();

        // Starts the log stream mutator
        logStream = new LogStream(this, builder.isFileLogger(ActivateType.ENABLED), builder.getLogRotate());
        logStream.start();

        // Log startup message
        logger.info("CraftsNet v" + version + " boots up");
        Runtime.Version jvmVersion = Runtime.version();
        logger.debug("JVM Version: " + jvmVersion.toString() + "; Max recognizable class file version: " + (jvmVersion.feature() + 44) + "." + jvmVersion.interim());

        // Check if version is a release as the version check is disabled for experimental builds
        if (version.matches("^\\d+(?:\\.\\d+)*$"))
            Versions.verbalCheck(this);

        logger.debug("Preloading gson for faster processing");
        Json.empty();

        // Setup default uncaught exception handler
        this.oldDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (logStream != null) {
                long identifier = logStream.createErrorLog(this, e);
                logger.error(e, "Throwable: " + identifier);
                return;
            }

            logger.error(e);
        });
        logger.debug("Injected the default uncaught exception handler");

        // Setup the file helper
        this.fileHelper = new FileHelper(this, builder.shouldPlaceTempFilesOnNormalFileSystem());

        // Initialize listener and route registries, and addon manager
        logger.info("Initialization of system variables");
        logger.debug("Initialization of the session cache");
        this.sessionCache = new SessionCache(builder.getSessionCacheSize());

        logger.debug("Initialization of the stream encoder registry");
        streamEncoderRegistry = new StreamEncoderRegistry();

        logger.debug("Initialization of the listener registry");
        listenerRegistry = new ListenerRegistry();

        logger.debug("Initialization of the middleware registry");
        middlewareRegistry = new MiddlewareRegistry();

        logger.debug("Initialization of the route registry");
        routeRegistry = new RouteRegistry(this);

        logger.debug("Initialization of the requirement registry");
        requirementRegistry = new RequirementRegistry(this);

        logger.debug("Initialization of the command registry");
        commandRegistry = new CommandRegistry(this);

        logger.debug("Initialization of the service manager");
        serviceManager = new ServiceManager(this);

        logger.debug("Initialization of the auto register registry");
        autoRegisterRegistry = new AutoRegisterRegistry(this);

        logger.debug("Initialization of the websocket extension registry");
        webSocketExtensionRegistry = new WebSocketExtensionRegistry();

        logger.debug("Initialization of the body registry");
        bodyRegistry = new BodyRegistry();

        logger.debug("Initialization of the addon manager");
        addonManager = new AddonManager(this);

        logger.info("Preparing the webserver");
        webServer = new WebServer(this, builder.getWebServerPort(), builder.isSSL());

        logger.info("Preparing the websocket server");
        webSocketServer = new WebSocketServer(this, builder.getWebSocketServerPort(), builder.isSSL());

        if (!builder.isAddonSystem(ActivateType.DISABLED)) {
            addonManager.fromFiles();

            if (builder instanceof AddonContainingBuilder addonBuilder)
                addonBuilder.loadAddons(this);

            addonManager.startup();
        }

        // Check if http routes are registered and start the web server if needed
        if (builder.isWebServer(ActivateType.ENABLED) || builder.isWebServer(ActivateType.DYNAMIC)) {
            // Register a default route if nothing has been registered.
            if (!builder.shouldSkipDefaultRoute() && !routeRegistry.hasRoutes() && !routeRegistry.hasWebsockets()) {
                logger.debug("No routes and sockets found, creating the default route");
                getRouteRegistry().register(DefaultRoute.getInstance());
            }

            // Start the webserver if needed
            if (routeRegistry.hasRoutes() || builder.isWebServer(ActivateType.ENABLED)) {
                webServer.start();
            }
        } else if (builder.isWebServer(ActivateType.DISABLED) && routeRegistry.hasRoutes())
            logger.warning("The web server is forcible disabled, but has registered routes!");

        // Check if webSocket routes are registered and start the websocket server if needed
        if (builder.isWebSocketServer(ActivateType.ENABLED) || builder.isWebSocketServer(ActivateType.DYNAMIC)) {
            logger.debug("Implementing the default ping responder");
            DefaultPingResponder.register(this);

            if (routeRegistry.hasWebsockets() || builder.isWebSocketServer(ActivateType.ENABLED))
                webSocketServer.start();
        } else if (builder.isWebSocketServer(ActivateType.DISABLED) && routeRegistry.hasWebsockets())
            logger.warning("The websocket server is forcible disabled, but has registered endpoints!");

        // Register build in commands / listeners
        if (!builder.isCommandSystem(ActivateType.DISABLED)) {
            logger.debug("Registering the console listener");
            listenerRegistry.register(new ConsoleListener(this));

            // Set up and start the console listener
            this.consoleReader = createConsoleReader();
            this.consoleListener = startConsoleListener();
            if (this.consoleListener != null) logger.debug("Started the console reader");
        }

        // Register a shutdown hook for calling the stop method
        this.shutdownThread = new Thread(this::stop, "CraftsNet Shutdown");
        Runtime.getRuntime().addShutdownHook(this.shutdownThread);
        logger.debug("JVM Shutdown Hook is implemented");

        // Add all with @AutoRegister annotated classes from the current jar file to the list
        try (AutoRegisterLoader autoRegisterLoader = new AutoRegisterLoader()) {
            for (CodeSource codeSource : builder.getCodeSources())
                try {
                    Path path = Path.of(codeSource.getLocation().toURI());
                    try (JarFile file = fileHelper.getJarFileAt(path)) {
                        var handlers = autoRegisterLoader.loadFrom(null, null, file);
                        autoRegisterRegistry.handleAll(handlers);
                        handlers.clear();
                    } catch (NoSuchFileException ignored) {
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
        }

        // Log successful startup message with elapsed time
        logger.info("CraftsNet was successfully started after " + (System.currentTimeMillis() - start) + "ms");
        started = true;
    }

    /**
     * Stops the CraftsNet framework.
     */
    public void stop() {
        logger.info("Shutdown request has been received");
        if (this.consoleListener != null && !this.consoleListener.isInterrupted()) {
            logger.info("Closing the console input listener");

            try {
                if (this.consoleReader != null) consoleReader.close();
            } catch (IOException ignored) {
            }

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

        if (this.sessionCache != null) {
            this.sessionCache.clear();
            this.sessionCache = null;
        }

        if (this.oldDefaultUncaughtExceptionHandler != null) {
            logger.debug("Resetting the default uncaught exception handler");
            Thread.setDefaultUncaughtExceptionHandler(this.oldDefaultUncaughtExceptionHandler);
        }

        if (this.logStream != null) {
            logger.info("Disconnecting the file logger");
            this.logStream.stop();
            this.logStream = null;
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
        started = false;
    }

    /**
     * Restarts the CraftsNet framework.
     */
    public void restart() {
        restart(null);
    }

    /**
     * Restarts the CraftsNet framework.
     *
     * @param executeBetween A {@link Runnable} which is before the new instance is started.
     */
    public void restart(Runnable executeBetween) {
        CraftsNetBuilder builder = this.builder;
        this.builder = null;
        Thread restart = new Thread(() -> {
            stop();
            if (executeBetween != null) executeBetween.run();
            try {
                start(builder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "CraftsNet Main");
        restart.start();

        try {
            restart.join();
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Creates a new {@link BufferedReader} that reads from the {@link System#in} input steam.
     * The instance is modified in a way that it does not close the underlying input stream
     * when it is closed.
     *
     * @return The new {@link BufferedReader} that reads from {@link System#in}.
     */
    private BufferedReader createConsoleReader() {
        if (this.consoleReader != null) return this.consoleReader;

        return new BufferedReader(new InputStreamReader(System.in)) {
            @Override
            public void close() {
                consoleReader = null;
            }
        };
    }

    /**
     * Creates a new thread that listens to the console and performs the according events.
     *
     * @return The console reader thread.
     */
    @Nullable
    private Thread startConsoleListener() {
        if (System.in == null) {
            logger.error("Console input stream not available!");
            return null;
        }

        Thread console = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (consoleReader == null) break;

                    String line = consoleReader.readLine();
                    if (line == null) {
                        getLogger().error("Unexpected console input: null");
                        getLogger().error("Console reader will be closed after this!");
                        break;
                    }

                    listenerRegistry.call(new ConsoleMessageEvent(line));
                }
            } catch (IOException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }, "CraftsNet Console Reader");
        console.setDaemon(true);
        console.start();
        return console;
    }

    /**
     * Retrieves the addon manager instance for managing addons.
     *
     * @return The addon manager instance.
     * @deprecated Use {@link #getAddonManager()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public AddonManager addonManager() {
        return this.getAddonManager();
    }

    /**
     * Retrieves the addon manager instance for managing addons.
     *
     * @return The addon manager instance.
     */
    public AddonManager getAddonManager() {
        return addonManager;
    }

    /**
     * Retrieves the auto register registry instance for auto registrable types.
     *
     * @return The auto register registry instance.
     * @since 3.2.0-SNAPSHOT
     * @deprecated Use {@link #getAutoRegisterRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public AutoRegisterRegistry autoRegisterRegistry() {
        return this.getAutoRegisterRegistry();
    }

    /**
     * Retrieves the auto register registry instance for auto registrable types.
     *
     * @return The auto register registry instance.
     * @since 3.2.0-SNAPSHOT
     */
    public AutoRegisterRegistry getAutoRegisterRegistry() {
        return autoRegisterRegistry;
    }

    /**
     * Retrieves the body registry instance for manging body types.
     *
     * @return The body registry instance.
     * @since 3.0.4-SNAPSHOT
     * @deprecated Use {@link #getBodyRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public BodyRegistry bodyRegistry() {
        return this.getBodyRegistry();
    }

    /**
     * Retrieves the body registry instance for manging body types.
     *
     * @return The body registry instance.
     * @since 3.0.4-SNAPSHOT
     */
    public BodyRegistry getBodyRegistry() {
        return bodyRegistry;
    }

    /**
     * Retrieves the command registry instance for managing commands.
     *
     * @return The command registry instance.
     * @deprecated Use {@link #getCommandRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    /**
     * Retrieves the command registry instance for managing commands.
     *
     * @return The command registry instance.
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Retrieves the listener registry instance for managing event listeners.
     *
     * @return The listener registry instance.
     * @deprecated Use {@link #getListenerRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public ListenerRegistry listenerRegistry() {
        return this.getListenerRegistry();
    }

    /**
     * Retrieves the listener registry instance for managing event listeners.
     *
     * @return The listener registry instance.
     */
    public ListenerRegistry getListenerRegistry() {
        return listenerRegistry;
    }

    /**
     * Retrieves the middleware registry instance for manging
     * {@link de.craftsblock.craftsnet.api.middlewares.Middleware middlewares}
     *
     * @return The {@link MiddlewareRegistry middleware registry} instance.
     * @since 3.4.0-SNAPSHOT
     * @deprecated Use {@link #getMiddlewareRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public MiddlewareRegistry middlewareRegistry() {
        return middlewareRegistry;
    }

    /**
     * Retrieves the middleware registry instance for manging
     * {@link de.craftsblock.craftsnet.api.middlewares.Middleware middlewares}
     *
     * @return The {@link MiddlewareRegistry middleware registry} instance.
     * @since 3.4.0-SNAPSHOT
     */
    public MiddlewareRegistry getMiddlewareRegistry() {
        return middlewareRegistry;
    }

    /**
     * Retrieves the route registry instance for managing web and web requirements.
     *
     * @return The requirement registry instance.
     * @since 3.2.1-SNAPSHOT
     * @deprecated Use {@link #getRequirementRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public RequirementRegistry requirementRegistry() {
        return this.getRequirementRegistry();
    }

    /**
     * Retrieves the route registry instance for managing web and web requirements.
     *
     * @return The requirement registry instance.
     * @since 3.2.1-SNAPSHOT
     */
    public RequirementRegistry getRequirementRegistry() {
        return requirementRegistry;
    }

    /**
     * Retrieves the route registry instance for managing web and web socket routes.
     *
     * @return The route registry instance.
     * @deprecated Use {@link #getRouteRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public RouteRegistry routeRegistry() {
        return this.getRouteRegistry();
    }

    /**
     * Retrieves the route registry instance for managing web and web socket routes.
     *
     * @return The route registry instance.
     */
    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
    }

    /**
     * Retrieves the service manager instance for managing services.
     *
     * @return The service manager instance.
     * @deprecated Use {@link #getServiceManager()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public ServiceManager serviceManager() {
        return this.getServiceManager();
    }

    /**
     * Retrieves the service manager instance for managing services.
     *
     * @return The service manager instance.
     */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    /**
     * Retrieves the session cache instance which is in charge of caching session.
     *
     * @return The session cache instance.
     * @deprecated Use {@link #getSessionCache()} ()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public SessionCache sessionCache() {
        return this.getSessionCache();
    }

    /**
     * Retrieves the session cache instance which is in charge of caching session.
     *
     * @return The session cache instance.
     */
    public SessionCache getSessionCache() {
        return sessionCache;
    }

    /**
     * Retrieves the stream encoder registry instance for managing stream encoders.
     *
     * @return The stream encoder registry instance.
     * @deprecated Use {@link #getStreamEncoderRegistry()} ()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public StreamEncoderRegistry streamEncoderRegistry() {
        return this.getStreamEncoderRegistry();
    }

    /**
     * Retrieves the stream encoder registry instance for managing stream encoders.
     *
     * @return The stream encoder registry instance.
     */
    public StreamEncoderRegistry getStreamEncoderRegistry() {
        return streamEncoderRegistry;
    }

    /**
     * Retrieves the extension registry for the websocket protocol.
     *
     * @return The websocket extension registry.
     * @deprecated Use {@link #getFileHelper()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public WebSocketExtensionRegistry webSocketExtensionRegistry() {
        return this.getWebSocketExtensionRegistry();
    }

    /**
     * Retrieves the extension registry for the websocket protocol.
     *
     * @return The websocket extension registry.
     */
    public WebSocketExtensionRegistry getWebSocketExtensionRegistry() {
        return webSocketExtensionRegistry;
    }

    /**
     * Retrieves the web server instance for handling HTTP requests.
     *
     * @return The web server instance.
     * @deprecated Use {@link #getWebServer()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public WebServer webServer() {
        return webServer;
    }

    /**
     * Retrieves the web server instance for handling HTTP requests.
     *
     * @return The web server instance.
     */
    public WebServer getWebServer() {
        return webServer;
    }

    /**
     * Retrieves the WebSocket server instance for handling WebSocket connections.
     *
     * @return The WebSocket server instance.
     * @deprecated Use {@link #getWebSocketServer()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public WebSocketServer webSocketServer() {
        return this.getWebSocketServer();
    }

    /**
     * Retrieves the WebSocket server instance for handling WebSocket connections.
     *
     * @return The WebSocket server instance.
     */
    public WebSocketServer getWebSocketServer() {
        return webSocketServer;
    }

    /**
     * Retrieves the logger instance for logging messages.
     *
     * @return The logger instance.
     * @deprecated Use {@link #getLogger()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public Logger logger() {
        return this.getLogger();
    }

    /**
     * Retrieves the logger instance for logging messages.
     *
     * @return The logger instance.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Retrieves the {@link LogStream} instance for advanced log creation.
     *
     * @return The {@link LogStream} instance.
     * @deprecated Use {@link #getLogStream()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public LogStream logStream() {
        return this.getLogStream();
    }

    /**
     * Retrieves the {@link LogStream} instance for advanced log creation.
     *
     * @return The {@link LogStream} instance.
     */
    public LogStream getLogStream() {
        return logStream;
    }

    /**
     * Retrieves the builder instance used for configuring CraftsNet.
     *
     * @return The builder instance.
     */
    public CraftsNetBuilder getBuilder() {
        return builder;
    }

    /**
     * Returns the {@link FileHelper} instance used for handling temporary files.
     *
     * @return the {@link FileHelper} instance.
     * @deprecated Use {@link #getFileHelper()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    public FileHelper fileHelper() {
        return this.getFileHelper();
    }

    /**
     * Returns the {@link FileHelper} instance used for handling temporary files.
     *
     * @return the {@link FileHelper} instance.
     */
    public FileHelper getFileHelper() {
        return fileHelper;
    }

    /**
     * Creates a new builder instance for configuring CraftsNet.
     *
     * @return A new builder instance.
     */
    public static CraftsNetBuilder create() {
        return new CraftsNetBuilder()
                .addCodeSource(ReflectionUtils.getCallerClass().getProtectionDomain().getCodeSource());
    }

    /**
     * Creates a new builder instance for configuring CraftsNet with the specified addons.
     * <p>
     * <b>Warning:</b> There is currently a bug where autoregister classes gets mixed up when
     * more than one addon is in the same jar file. Use with caution in production!
     *
     * @param addons An array of {@link Addon} classes to include in the configuration.
     * @return A new {@link AddonContainingBuilder} instance initialized with the specified addons.
     */
    @SafeVarargs
    public static AddonContainingBuilder create(Class<? extends Addon>... addons) {
        return CraftsNet.create(List.of(addons));
    }

    /**
     * Creates a new builder instance for configuring CraftsNet with the specified addons.
     * <p>
     * <b>Warning:</b> There is currently a bug where autoregister classes gets mixed up when
     * more than one addon is in the same jar file. Use with caution in production!
     *
     * @param addons A {@link Collection} of {@link Addon} classes to include in the configuration.
     * @return A new {@link AddonContainingBuilder} instance initialized with the specified addons.
     */
    public static AddonContainingBuilder create(Collection<Class<? extends Addon>> addons) {
        return new AddonContainingBuilder(addons)
                .addCodeSource(ReflectionUtils.getCallerClass().getProtectionDomain().getCodeSource());
    }

}
