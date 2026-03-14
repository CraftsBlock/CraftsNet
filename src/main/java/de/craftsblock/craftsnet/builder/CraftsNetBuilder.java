package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftscore.utils.ArgumentParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.builder.server.ServerBuilder;
import de.craftsblock.craftsnet.builder.server.ServerState;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.logging.impl.LoggerImpl;
import de.craftsblock.craftsnet.logging.impl.PlainLogger;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.CodeSource;
import java.util.*;

/**
 * Builder class for configuring the CraftsNet.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see ActivateType
 * @since 3.0.3-SNAPSHOT
 */
public class CraftsNetBuilder {


    private final List<CodeSource> codeSources = new ArrayList<>();

    private final ServerBuilder webServerBuilder;
    private final ServerBuilder webSocketServerBuilder;

    private ActivateType addonSystem;

    private int sessionCacheSize;

    private ActivateType fileLogger;
    private Logger logger;

    private List<String> trustedProxyHeaders;
    private Collection<String> trustedProxyHeadersView;

    private boolean allowResponseEncoding;
    private boolean debug;
    private boolean hideIpsInLog;
    private boolean tempFilesOnNormalFileSystem;
    private boolean skipDefaultRoute;
    private boolean skipVersionCheck;
    private boolean ssl;

    private long logRotate;

    /**
     * Constructs a new {@link CraftsNetBuilder} instance with default configuration settings.
     */
    public CraftsNetBuilder() {
        this.webServerBuilder = new ServerBuilder(this, ServerState.DYNAMIC, 5000);
        this.webSocketServerBuilder = new ServerBuilder(this, ServerState.DYNAMIC, 5001);

        addonSystem = fileLogger = ActivateType.ENABLED;
        withSessionCache(5);
        withDebug(false);
        withIpsInLog(true);
        withApplyResponseEncoding(false);
        withTempFilesOnNormalFileSystem(false);
        withSkipDefaultRoute(false);
        withSkipVersionCheck(false);
        withSSL(false);
        withoutLogRotate();
        withTrustedProxyHeaders(new ArrayList<>());

        addCodeSource(this.getClass().getProtectionDomain().getCodeSource());
    }

    public ServerBuilder webServerBuilder() {
        return webServerBuilder;
    }

    public ServerBuilder webSocketServerBuilder() {
        return webSocketServerBuilder;
    }

    /**
     * Configures the builder using a set of command-line arguments.
     * <p>
     * Parses the provided arguments and applies corresponding settings to the builder.
     * If a specific argument is not recognized, an exception is thrown.
     * </p>
     *
     * @param args An array of command-line arguments to parse.
     * @return The current instance of {@link CraftsNetBuilder} for chaining.
     * @throws RuntimeException If there is an error accessing the underlying argument parser's data.
     */
    public CraftsNetBuilder withArgs(String[] args) {
        ArgumentParser parser = new ArgumentParser(args);

        try {
            Field field = ReflectionUtils.findField(parser.getClass(), "arguments");

            if (field == null)
                throw new IllegalStateException("Can not load the arguments field!");

            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, String> arguments = (Map<String, String>) field.get(parser);
            arguments.forEach(this::setArg);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not unpack arguments!", e);
        }

        return this;
    }

    /**
     * Applies a single argument and its value to the builder.
     * <p>
     * Recognizes a predefined set of argument names and updates the corresponding settings.
     * If the argument is not recognized, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @param arg   The argument name, case-insensitive.
     * @param value The argument value, used for arguments requiring additional data.
     * @throws IllegalStateException If the argument name is not recognized.
     */
    protected void setArg(String arg, String value) {
        switch (arg.toLowerCase()) {
            // Flags
            case "debug" -> withDebug(true);
            case "hideips", "hideipsinlog" -> withIpsInLog(false);
            case "placetempfileinnormal" -> withTempFilesOnNormalFileSystem(true);
            case "skipdefaultroute" -> withSkipDefaultRoute(true);
            case "skipversioncheck" -> withSkipVersionCheck(true);
            case "ssl" -> withSSL(true);

            case "disableaddonsystem" -> withAddonSystem(ActivateType.DISABLED);
            case "disablefilelogger" -> withFileLogger(ActivateType.DISABLED);
            case "disablelogger" -> withLogger(ActivateType.DISABLED);
            case "disablelogrotate" -> withoutLogRotate();
            case "disablewebserver" -> withWebServer(ActivateType.DISABLED);
            case "disablewebsocketserver" -> withWebSocketServer(ActivateType.DISABLED);

            case "forcewebserver" -> withWebServer(ActivateType.ENABLED);
            case "forcewebsocketserver" -> withWebSocketServer(ActivateType.ENABLED);

            case "enableresponseencoding" -> withApplyResponseEncoding(true);

            // Arguments
            case "http-port", "httpport" -> withWebServer(Integer.parseInt(value));
            case "log-rotate", "logrotate" -> withLogRotate(Integer.parseInt(value));
            case "socket-port", "socketport", "websocket-port", "websocketport" -> withWebSocketServer(Integer.parseInt(value));

            case "sessioncache", "sessioncachesize" -> withSessionCache(Integer.parseInt(value));

            // Default
            default -> throw new IllegalStateException("Unexpected argument in startup command: " + arg.toLowerCase());
        }
    }

    /**
     * Adds a {@link CodeSource} to the code sources list.
     *
     * @param codeSource The {@link CodeSource} that should be added.
     * @return The {@link CraftsNetBuilder} instance.
     */
    public CraftsNetBuilder addCodeSource(CodeSource codeSource) {
        if (codeSources.contains(codeSource)) return this;
        this.codeSources.add(codeSource);
        return this;
    }

    /**
     * Removes a {@link CodeSource} from the code sources list.
     *
     * @param codeSource The {@link CodeSource} that should be removed.
     * @return The {@link CraftsNetBuilder} instance.
     */

    public CraftsNetBuilder removeCodeSource(CodeSource codeSource) {
        this.codeSources.remove(codeSource);
        return this;
    }

    /**
     * Specifies the port for the web server.
     *
     * @param port The port number for the web server.
     * @return The {@link CraftsNetBuilder} instance.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public CraftsNetBuilder withWebServer(int port) {
        return withWebServer(ActivateType.DYNAMIC, port);
    }

    /**
     * Specifies the port for the web server.
     *
     * @param type The activation type for the web server.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.0.5-SNAPSHOT
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public CraftsNetBuilder withWebServer(ActivateType type) {
        return withWebServer(type, this.webServerBuilder.port());
    }

    /**
     * Specifies the activation type and port for the web server.
     *
     * @param type The activation type for the web server.
     * @param port The port number for the web server.
     * @return The {@link CraftsNetBuilder} instance.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public CraftsNetBuilder withWebServer(ActivateType type, int port) {
        this.webServerBuilder.port(port)
                .state(switch (type) {
                    case ENABLED -> ServerState.FORCE;
                    case DYNAMIC -> ServerState.DYNAMIC;
                    case DISABLED -> ServerState.DISABLED;
                });
        return this;
    }

    /**
     * Specifies the port for the WebSocket server.
     *
     * @param port The port number for the WebSocket server.
     * @return The {@link CraftsNetBuilder} instance.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public CraftsNetBuilder withWebSocketServer(int port) {
        return withWebSocketServer(ActivateType.DYNAMIC, port);
    }

    /**
     * Specifies the activation type and port for the WebSocket server.
     *
     * @param type The activation type for the WebSocket server.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.0.5-SNAPSHOT
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public CraftsNetBuilder withWebSocketServer(ActivateType type) {
        return withWebSocketServer(type, this.webSocketServerBuilder.port());
    }

    /**
     * Specifies the activation type and port for the WebSocket server.
     *
     * @param type The activation type for the WebSocket server.
     * @param port The port number for the WebSocket server.
     * @return The {@link CraftsNetBuilder} instance.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public CraftsNetBuilder withWebSocketServer(ActivateType type, int port) {
        this.webSocketServerBuilder.port(port)
                .state(switch (type) {
                    case ENABLED -> ServerState.FORCE;
                    case DYNAMIC -> ServerState.DYNAMIC;
                    case DISABLED -> ServerState.DISABLED;
                });
        return this;
    }

    /**
     * Specifies the activation type for the addon system.
     *
     * @param type The activation type for the addon system.
     * @return The {@link CraftsNetBuilder} instance.
     */
    public CraftsNetBuilder withAddonSystem(ActivateType type) {
        this.addonSystem = type;
        return this;
    }

    /**
     * Specifies the size of the session cache.
     *
     * @param size The size of the session cache.
     * @return The {@link CraftsNetBuilder} instance.
     */
    public CraftsNetBuilder withSessionCache(int size) {
        this.sessionCacheSize = size;
        return this;
    }

    /**
     * Specifies the activation type for the file logger.
     *
     * @param type The activation type for the file logger.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.0.5-SNAPSHOT
     */
    public CraftsNetBuilder withFileLogger(ActivateType type) {
        this.fileLogger = type;
        return this;
    }

    /**
     * Specifies whether from the client requested response encodings should be applied or not.
     *
     * @param allowed {@code true} if the requested response encoding should be applied, {@code false} otherwise.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.3.3-SNAPSHOT
     */
    public CraftsNetBuilder withApplyResponseEncoding(boolean allowed) {
        this.allowResponseEncoding = allowed;
        return this;
    }

    /**
     * Configures whether temporary files should be placed on the normal file system.
     * When set to {@code true}, the application will store temporary files in the local
     * file system instead of using the system's default temporary file location.
     *
     * @param tempFilesOnNormalFileSystem {@code true} to place temporary files in the normal file system, {@code false} otherwise.
     * @return the current {@link CraftsNetBuilder} instance for method chaining.
     */
    public CraftsNetBuilder withTempFilesOnNormalFileSystem(boolean tempFilesOnNormalFileSystem) {
        this.tempFilesOnNormalFileSystem = tempFilesOnNormalFileSystem;
        return this;
    }

    /**
     * Specifies whether the registration of the default route (if no other route was registered)
     * should be skipped on startup.
     *
     * @param skip {@code true} if the version check should be skipped, {@code false} otherwise.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.3.3-SNAPSHOT
     */
    public CraftsNetBuilder withSkipDefaultRoute(boolean skip) {
        this.skipDefaultRoute = skip;
        return this;
    }

    /**
     * Specifies whether the version check should be skipped on startup.
     *
     * @param skip {@code true} if the version check should be skipped, false otherwise.
     * @return The {@link CraftsNetBuilder} instance.
     */
    public CraftsNetBuilder withSkipVersionCheck(boolean skip) {
        this.skipVersionCheck = skip;
        return this;
    }

    /**
     * Sets a custom logger which will be used by CraftsNet. It can be null when the default logger should be used.
     *
     * @param logger The instance of the custom logger.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.0.5-SNAPSHOT
     */
    public CraftsNetBuilder withCustomLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Specifies the activation type for the logger.
     *
     * @param type The activation type for the logger.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.0.5-SNAPSHOT
     */
    public CraftsNetBuilder withLogger(ActivateType type) {
        if (type.equals(ActivateType.DISABLED)) {
            this.logger = this.logger instanceof PlainLogger ? this.logger : new PlainLogger(this.logger);
        } else if (this.logger instanceof PlainLogger logger) {
            this.logger = logger.previous();
        }

        return this;
    }

    public CraftsNetBuilder withoutTrustedProxyHeaders() {
        if (this.trustedProxyHeaders == null) {
            return this;
        }

        this.trustedProxyHeaders.clear();
        return this;
    }

    public CraftsNetBuilder withTrustedProxyHeaders(String... trustedProxyHeaders) {
        this.withTrustedProxyHeaders(List.of(trustedProxyHeaders));
        return this;
    }

    public CraftsNetBuilder withTrustedProxyHeaders(List<String> trustedProxyHeaders) {
        this.trustedProxyHeaders = new ArrayList<>(trustedProxyHeaders);
        this.trustedProxyHeadersView = Collections.unmodifiableList(this.trustedProxyHeaders);
        return this;
    }

    /**
     * Specifies whether debug mode should be enabled.
     *
     * @param enabled {@code true} if debug mode should be enabled, {@code false} otherwise.
     * @return The {@link CraftsNetBuilder} instance.
     */
    public CraftsNetBuilder withDebug(boolean enabled) {
        this.debug = enabled;
        return this;
    }

    /**
     * Specifies whether ips should be blurred in the log output.
     *
     * @param enabled {@code true} if ips should be readable in the log, {@code false} otherwise.
     * @return The {@link CraftsNetBuilder} instance.
     * @since 3.4.0-SNAPSHOT
     */
    public CraftsNetBuilder withIpsInLog(boolean enabled) {
        this.hideIpsInLog = !enabled;
        return this;
    }

    /**
     * Specifies whether SSL should be enabled.
     *
     * @param enabled {@code true} if SSL should be enabled, {@code false} otherwise.
     * @return The {@link CraftsNetBuilder} instance.
     */
    public CraftsNetBuilder withSSL(boolean enabled) {
        ssl = enabled;
        return this;
    }

    /**
     * Sets the log rotation size threshold for this builder.
     *
     * @param logRotate the log rotation size threshold, in bytes. A value of 0 disables log rotation.
     *                  Must be non-negative.
     * @return this {@link CraftsNetBuilder} instance, enabling method chaining.
     */
    public CraftsNetBuilder withLogRotate(@Range(from = 0, to = Long.MAX_VALUE) long logRotate) {
        this.logRotate = logRotate;
        return this;
    }

    /**
     * Disables log rotation by setting the log rotation size threshold to 0.
     *
     * @return this {@link CraftsNetBuilder} instance, enabling method chaining.
     */
    public CraftsNetBuilder withoutLogRotate() {
        return withLogRotate(0);
    }

    /**
     * Retrieves the list of {@link CodeSource} that should be taken into account.
     *
     * @return The list containing {@link CodeSource}.
     */
    public Collection<CodeSource> getCodeSources() {
        return Collections.unmodifiableCollection(codeSources);
    }

    /**
     * Retrieves the port number configured for the web server.
     *
     * @return The port number for the web server.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public int getWebServerPort() {
        return this.webServerBuilder.port();
    }

    /**
     * Retrieves the activation type configured for the web server.
     *
     * @return The activation type for the web server.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public ActivateType getWebServer() {
        return switch (this.webServerBuilder.state()) {
            case FORCE -> ActivateType.ENABLED;
            case DYNAMIC -> ActivateType.DYNAMIC;
            case DISABLED -> ActivateType.DISABLED;
        };
    }

    /**
     * Checks if the web server is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the web server is configured with the specified activation type, false otherwise.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public boolean isWebServer(ActivateType type) {
        return this.webServerBuilder.state() == switch (type) {
            case ENABLED -> ServerState.FORCE;
            case DYNAMIC -> ServerState.DYNAMIC;
            case DISABLED -> ServerState.DISABLED;
        };
    }

    /**
     * Retrieves the port number configured for the WebSocket server.
     *
     * @return The port number for the WebSocket server.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public int getWebSocketServerPort() {
        return this.webSocketServerBuilder.port();
    }

    /**
     * Retrieves the activation type configured for the WebSocket server.
     *
     * @return The activation type for the WebSocket server.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public ActivateType getWebSocketServer() {
        return switch (this.webSocketServerBuilder.state()) {
            case FORCE -> ActivateType.ENABLED;
            case DYNAMIC -> ActivateType.DYNAMIC;
            case DISABLED -> ActivateType.DISABLED;
        };
    }

    /**
     * Checks if the WebSocket server is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the WebSocket server is configured with the specified activation type, false otherwise.
     */
    @Deprecated(since = "3.7.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "3.8.0")
    public boolean isWebSocketServer(ActivateType type) {
        return this.webSocketServerBuilder.state() == switch (type) {
            case ENABLED -> ServerState.FORCE;
            case DYNAMIC -> ServerState.DYNAMIC;
            case DISABLED -> ServerState.DISABLED;
        };
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
     * Retrieves the session cache size configured.
     *
     * @return The session cache size.
     */
    public int getSessionCacheSize() {
        return sessionCacheSize;
    }

    /**
     * Checks if the file logger is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the file logger is configured with the specified activation type, false otherwise.
     * @since 3.0.5-SNAPSHOT
     */
    public boolean isFileLogger(ActivateType type) {
        return fileLogger == type;
    }

    /**
     * Determines whether from the client requested response encoding should be applied or not.
     *
     * @return {@code true} if the request encoding should be applied, {@code false} otherwise.
     * @since 3.3.3-SNAPSHOT
     */
    public boolean responseEncodingAllowed() {
        return allowResponseEncoding;
    }

    /**
     * Determines whether temporary files should be placed in the normal file system.
     *
     * @return {@code true} if temporary files are placed on the normal file system, {@code false} otherwise.
     */
    public boolean shouldPlaceTempFilesOnNormalFileSystem() {
        return tempFilesOnNormalFileSystem;
    }

    /**
     * Determines whether the registration of the default route (if no other route was registered)
     * should be skipped on startup.
     *
     * @return {@code true} if the registration of the default rout should be skipped, {@code false} otherwise.
     * @since 3.3.3-SNAPSHOT
     */
    public boolean shouldSkipDefaultRoute() {
        return skipDefaultRoute;
    }

    /**
     * Determines whether the version check should be skipped on startup.
     *
     * @return {@code true} if the version check should be skipped, {@code false} otherwise.
     */
    public boolean shouldSkipVersionCheck() {
        return skipVersionCheck;
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

    public Collection<String> getTrustedProxyHeaders() {
        return trustedProxyHeadersView;
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return {@code true} if debug mode is enabled, {@code false} otherwise.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Checks if ips should be hidden in the log output.
     *
     * @return {@code true} if ips should be hidden, {@code false} otherwise.
     * @since 3.4.0-SNAPSHOT
     */
    public boolean shouldHideIps() {
        return hideIpsInLog;
    }

    /**
     * Checks if SSL is enabled.
     *
     * @return {@code true} if SSL is enabled, {@code false} otherwise.
     */
    public boolean isSSL() {
        return ssl;
    }

    /**
     * Retrieves the log rotation size threshold.
     *
     * @return the log rotation size threshold in bytes, or 0 if log rotation is disabled.
     */
    public long getLogRotate() {
        return logRotate;
    }

    /**
     * Checks if log rotation is disabled.
     *
     * @return {@code true} if log rotation is disabled (i.e., the threshold is 0), {@code false} otherwise.
     */
    public boolean isLogRotate() {
        return logRotate == 0;
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
