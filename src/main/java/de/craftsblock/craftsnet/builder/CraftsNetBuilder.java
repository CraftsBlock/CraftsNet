package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftscore.utils.Arguments;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.builder.addon.AddonBuilder;
import de.craftsblock.craftsnet.builder.addon.AddonSystemBuilder;
import de.craftsblock.craftsnet.builder.addon.AddonSystemState;
import de.craftsblock.craftsnet.builder.server.ServerBuilder;
import de.craftsblock.craftsnet.builder.server.ServerState;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.logging.impl.LoggerImpl;
import de.craftsblock.craftsnet.logging.impl.PlainLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Builder class for configuring the CraftsNet.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see ActivateType
 * @since 3.0.3-SNAPSHOT
 */
@SuppressWarnings("removal")
public class CraftsNetBuilder extends AbstractCraftsNetBuilder<CraftsNetBuilder> {

    private final List<CodeSource> codeSources = new ArrayList<>();

    private final Map<String, NamespacedBuilder<?>> internalBuilders;

    private final AddonSystemBuilder addonSystemBuilder;

    private final ServerBuilder webServerBuilder;
    private final ServerBuilder webSocketServerBuilder;

    private int sessionCacheSize;

    private ActivateType fileLogger;
    private Logger logger;

    private List<String> trustedProxyHeaders;
    private Collection<String> trustedProxyHeadersView;

    private boolean allowResponseEncoding;
    private boolean debug;
    private boolean hideIpsInLog;
    private boolean tempFilesOnNormalFileSystem;
    private boolean skipVersionCheck;

    private long logRotate;

    /**
     * Constructs a new {@link CraftsNetBuilder} instance with default configuration settings.
     */
    public CraftsNetBuilder() {
        acquire(this);

        this.internalBuilders = new HashMap<>();

        this.addonSystemBuilder = new AddonSystemBuilder(this, AddonSystemState.FULL);

        this.webServerBuilder = new ServerBuilder(this, "http", ServerState.DYNAMIC, 5000, false, false);
        this.webSocketServerBuilder = new ServerBuilder(this, "websocket", ServerState.DYNAMIC, 5001, false, false);

        fileLogger = ActivateType.ENABLED;
        withSessionCache(5);
        withDebug(false);
        withIpsInLog(true);
        withApplyResponseEncoding(false);
        withTempFilesOnNormalFileSystem(false);
        withSkipVersionCheck(false);
        withoutLogRotate();
        withTrustedProxyHeaders(new ArrayList<>());

        addCodeSource(this.getClass().getProtectionDomain().getCodeSource());
    }

    void registerInternalNamespace(NamespacedBuilder<?> builder) {
        this.internalBuilders.put(builder.getNamespace().toLowerCase(), builder);
    }

    public @NotNull CraftsNetBuilder setAll(String[] args) {
        Arguments arguments = new Arguments(args, Arguments.Pattern.SPACE_SEPARATED);
        arguments.getMapView().forEach(this::set);
        return this;
    }

    @Override
    public @NotNull CraftsNetBuilder set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");

        if (!arg.contains(":")) {
            return super.set(arg, value);
        }

        String[] parts = arg.split(":", 2);
        String namespace = parts[0];

        NamespacedBuilder<?> builder = this.internalBuilders.get(namespace.toLowerCase());
        if (builder == null) {
            throw new IllegalArgumentException("No internal namespace " + namespace
                    + " found for arg " + arg);
        }

        builder.set(parts[1], value);
        return this;
    }

    public CraftsNetBuilder webServer(Consumer<ServerBuilder> builderConsumer) {
        builderConsumer.accept(this.webServerBuilder);
        return this;
    }

    public ServerBuilder webServer() {
        return this.webServerBuilder;
    }

    public CraftsNetBuilder webSocketServer(Consumer<ServerBuilder> builderConsumer) {
        builderConsumer.accept(this.webSocketServerBuilder);
        return this;
    }

    public ServerBuilder webSocketServer() {
        return this.webSocketServerBuilder;
    }

    public CraftsNetBuilder addonSystem(Consumer<AddonSystemBuilder> builderConsumer) {
        builderConsumer.accept(this.addonSystemBuilder);
        return this;
    }

    public AddonSystemBuilder addonSystem() {
        return this.addonSystemBuilder;
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
