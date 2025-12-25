package de.craftsblock.craftsnet;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.codec.registry.TypeEncoderRegistry;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.http.body.BodyRegistry;
import de.craftsblock.craftsnet.api.http.builtin.DefaultRoute;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoderRegistry;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareRegistry;
import de.craftsblock.craftsnet.api.requirements.RequirementRegistry;
import de.craftsblock.craftsnet.api.session.SessionCache;
import de.craftsblock.craftsnet.api.websocket.DefaultPingResponder;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeEncoder;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtensionRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterRegistry;
import de.craftsblock.craftsnet.autoregister.loaders.AutoRegisterLoader;
import de.craftsblock.craftsnet.builder.ActivateType;
import de.craftsblock.craftsnet.builder.AddonContainingBuilder;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.logging.mutate.LogStream;
import de.craftsblock.craftsnet.utils.FileHelper;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import de.craftsblock.craftsnet.utils.versions.Versions;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

/**
 * CraftsNet class represents the core component of the CraftsNet framework,
 * providing functionalities for managing various aspects of the system.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 3.6.1
 * @since 1.0.0-SNAPSHOT
 */
public class CraftsNet {

    /**
     * The current version of CraftsNet.
     */
    public static final String version = "3.7.0-pre3";

    private static CraftsNet instance;

    // Local instance
    private CraftsNetBuilder builder;
    private Logger logger;
    private LogStream logStream;
    private FileHelper fileHelper;

    // Threads
    private Thread shutdownThread;
    private Thread.UncaughtExceptionHandler oldDefaultUncaughtExceptionHandler;

    // Manager instances
    private AddonManager addonManager;
    private AutoRegisterRegistry autoRegisterRegistry;
    private BodyRegistry bodyRegistry;
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
        if (started) {
            throw new RuntimeException("The instance of CraftsNet has already been started!");
        }
        this.builder = builder;

        // Start measuring the startup time
        long start = System.currentTimeMillis();

        logger = builder.getCustomLogger();

        logStream = new LogStream(this, builder.isFileLogger(ActivateType.ENABLED), builder.getLogRotate());
        logStream.start();

        if (instance != null) {
            Arrays.stream(new Logger[]{logger, instance.logger}).forEach(log -> {
                log.warning("Detected another instance of CraftsNet in the jvm!");
                log.warning("This may cause some errors.");
            });
        }

        instance = this;

        logger.info("CraftsNet v%s boots up", version);
        Runtime.Version jvmVersion = Runtime.version();
        logger.debug("JVM Version: %s; Max recognizable class file version: %s.%s",
                jvmVersion.toString(), jvmVersion.feature() + 44, jvmVersion.interim());

        if (!builder.shouldSkipVersionCheck() && version.matches("^\\d+(?:\\.\\d+)*$")) {
            Versions.verbalCheck(this);
        }

        logger.debug("Preloading gson for faster processing");
        Json.empty();

        this.oldDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (logStream != null) {
                long identifier = logStream.createErrorLog(this, e);
                logger.error("Throwable: %s", e, identifier);
                return;
            }

            logger.error(e);
        });
        logger.debug("Injected the default uncaught exception handler");

        this.fileHelper = new FileHelper(this, builder.shouldPlaceTempFilesOnNormalFileSystem());

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

        logger.debug("Initialization of the service manager");
        serviceManager = new ServiceManager(this);

        logger.debug("Initialization of the websocket extension registry");
        webSocketExtensionRegistry = new WebSocketExtensionRegistry();

        logger.debug("Initialization of the body registry");
        bodyRegistry = new BodyRegistry();

        logger.debug("Initialization of the addon manager");
        addonManager = new AddonManager(this);

        logger.info("Preparing the webserver");
        webServer = new WebServer(this, builder.getWebServerPort(), 25, builder.isSSL());

        logger.info("Preparing the websocket server");
        webSocketServer = new WebSocketServer(this, builder.getWebSocketServerPort(), 25, builder.isSSL());

        logger.debug("Initialization of the auto register registry");
        autoRegisterRegistry = new AutoRegisterRegistry(this);

        if (!builder.isAddonSystem(ActivateType.DISABLED)) {
            addonManager.fromFiles();

            if (builder instanceof AddonContainingBuilder addonBuilder) {
                addonBuilder.loadAddons(this);
            }

            addonManager.startup();
        }

        if (builder.isWebServer(ActivateType.ENABLED) || builder.isWebServer(ActivateType.DYNAMIC)) {
            if (!builder.shouldSkipDefaultRoute() && !routeRegistry.hasRoutes() && !routeRegistry.hasWebsockets()) {
                logger.debug("No routes and sockets found, creating the default route");
                getRouteRegistry().register(DefaultRoute.getInstance());
            }

            if (routeRegistry.hasRoutes() || builder.isWebServer(ActivateType.ENABLED)) {
                webServer.start();
            }
        } else if (builder.isWebServer(ActivateType.DISABLED) && routeRegistry.hasRoutes()) {
            logger.warning("The web server is forcible disabled, but has registered routes!");
        }

        if (builder.isWebSocketServer(ActivateType.ENABLED) || builder.isWebSocketServer(ActivateType.DYNAMIC)) {
            logger.debug("Implementing the default ping responder");
            DefaultPingResponder.register(this);

            if (routeRegistry.hasWebsockets() || builder.isWebSocketServer(ActivateType.ENABLED)) {
                webSocketServer.start();
            }
        } else if (builder.isWebSocketServer(ActivateType.DISABLED) && routeRegistry.hasWebsockets()) {
            logger.warning("The websocket server is forcible disabled, but has registered endpoints!");
        }

        this.shutdownThread = new Thread(this::stop, "CraftsNet Shutdown");
        Runtime.getRuntime().addShutdownHook(this.shutdownThread);
        logger.debug("JVM Shutdown Hook is implemented");

        try (AutoRegisterLoader autoRegisterLoader = new AutoRegisterLoader()) {
            for (CodeSource codeSource : builder.getCodeSources()) {
                try {
                    Path path = Path.of(codeSource.getLocation().toURI());

                    try (JarFile file = fileHelper.getJarFileAt(path)) {
                        var handlers = autoRegisterLoader.loadFrom(null, null, file);
                        autoRegisterRegistry.handleAll(handlers);
                        handlers.clear();
                    }
                } catch (NoSuchFileException ignored) {
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException("Could not autoregister from code sources!", e);
                }
            }
        }

        // Log successful startup message with elapsed time
        logger.info("CraftsNet was successfully started after %sms", System.currentTimeMillis() - start);
        started = true;
    }

    /**
     * Stops the CraftsNet framework.
     */
    public void stop() {
        logger.info("Shutdown request has been received");

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
        instance = null;
    }

    /**
     * Restarts the CraftsNet framework.
     *
     * @deprecated This method can create a mass amount of threads. There will be no replacement.
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    public void restart() {
        restart(null);
    }

    /**
     * Restarts the CraftsNet framework.
     *
     * @param executeBetween A {@link Runnable} which is before the new instance is started.
     * @deprecated This method can create a mass amount of threads. There will be no replacement.
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public void restart(Runnable executeBetween) {
        CraftsNetBuilder builder = this.builder;
        this.builder = null;
        Thread restart = new Thread(() -> {
            stop();

            if (executeBetween != null) {
                executeBetween.run();
            }

            try {
                start(builder);
            } catch (IOException e) {
                throw new RuntimeException("Could not restart CraftsNet properly!", e);
            }
        }, "CraftsNet Main");
        restart.start();

        try {
            restart.join();
        } catch (InterruptedException ignored) {
        }
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
     */
    public AutoRegisterRegistry getAutoRegisterRegistry() {
        return autoRegisterRegistry;
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
     */
    public MiddlewareRegistry getMiddlewareRegistry() {
        return middlewareRegistry;
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
     */
    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
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
     */
    public SessionCache getSessionCache() {
        return sessionCache;
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
     * Retrieves the {@link TypeEncoderRegistry} dedicated to managing
     * {@link WebSocketSafeTypeEncoder} instances used by the {@link WebSocketServer}.
     *
     * @return the {@link TypeEncoderRegistry} for {@link WebSocketSafeTypeEncoder} codecs
     */
    public TypeEncoderRegistry<WebSocketSafeTypeEncoder<?, ?>> getWebSocketEncoderRegistry() {
        return webSocketServer.getTypeEncoderRegistry();
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
     */
    public WebServer getWebServer() {
        return webServer;
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
     */
    public Logger getLogger() {
        return logger;
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
     */
    public FileHelper getFileHelper() {
        return fileHelper;
    }

    /**
     * Get the current instance of {@link CraftsNet}.
     *
     * @return The instance of {@link CraftsNet}.
     * @since 3.7.0
     */
    public static CraftsNet getInstance() {
        return instance;
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
