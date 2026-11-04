package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftscore.utils.Arguments;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.builder.addon.AddonSystemBuilder;
import de.craftsblock.craftsnet.builder.logging.LoggingBuilder;
import de.craftsblock.craftsnet.builder.server.ServerBuilder;
import de.craftsblock.craftsnet.builder.server.WebServerBuilder;
import de.craftsblock.craftsnet.builder.server.WebSocketServerBuilder;
import org.jetbrains.annotations.NotNull;

import java.security.CodeSource;
import java.util.*;
import java.util.function.Consumer;

/**
 * Main builder for configuring and creating a {@link CraftsNet} instance.
 * <p>
 * This builder provides access to the configuration of the add-on system, logging,
 * and server implementations. It also supports configuring general CraftsNet
 * properties, registering code sources, and applying configuration values parsed
 * from command-line arguments.
 * <p>
 * Namespaced configuration arguments are delegated to their corresponding internal
 * builders. Several aliases are supported for accessing these builders through
 * command-line configuration.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see AbstractCraftsNetBuilder
 * @see AddonSystemBuilder
 * @see LoggingBuilder
 * @see ServerBuilder
 * @see CraftsNet
 * @since 4.0.0
 */
public class CraftsNetBuilder extends AbstractCraftsNetBuilder<CraftsNetBuilder> {

    /**
     * Maps internal namespace names and aliases to their corresponding builders.
     */
    private final Map<String, NamespacedBuilder<?>> internalBuilders;

    /**
     * Builder responsible for configuring the add-on system.
     */
    private final AddonSystemBuilder addonSystemBuilder;

    /**
     * Builder responsible for configuring the logging system.
     */
    private final LoggingBuilder loggingBuilder;

    /**
     * Builder responsible for configuring the HTTP web server.
     */
    private final WebServerBuilder webServerBuilder;

    /**
     * Builder responsible for configuring the WebSocket server.
     */
    private final WebSocketServerBuilder webSocketServerBuilder;

    /**
     * Creates and initializes the main CraftsNet builder.
     * <p>
     * The constructor registers this builder, initializes the internal namespace
     * registry, registers the supported general configuration arguments, and creates
     * the builders for the add-on system, logging, and server components.
     * <p>
     * It also registers the namespace aliases {@code log}, {@code http}, and
     * {@code ws}, applies the default configuration values, and registers the
     * code source of this builder's class.
     */
    public CraftsNetBuilder() {
        acquire(this);

        this.internalBuilders = new HashMap<>();
        this.addKnownArgument("debug", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("codeSources", ArgumentTransformer.notAllowed("Can't add code sources via the cli"));
        this.addKnownArgument("sessionCacheSize", ArgumentTransformer.integerNumber(0, Integer.MAX_VALUE));
        this.addKnownArgument("skipVersionCheck", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("tempFilesInPwd", ArgumentTransformer.booleanFlag());

        this.addonSystemBuilder = new AddonSystemBuilder(this);

        this.loggingBuilder = new LoggingBuilder(this);
        this.registerInternalNamespaceAlias("log", this.loggingBuilder);

        this.webServerBuilder = new WebServerBuilder(this);
        this.registerInternalNamespaceAlias("http", this.webServerBuilder);

        this.webSocketServerBuilder = new WebSocketServerBuilder(this);
        this.registerInternalNamespaceAlias("ws", this.webSocketServerBuilder);

        this.debug(false);
        this.sessionCacheSize(5);
        this.skipVersionCheck(false);
        this.tempFilesInPwd(false);

        this.set("codeSources", new HashSet<>());
        this.addCodeSource(this.getClass().getProtectionDomain().getCodeSource());
    }

    /**
     * Registers an alias for an internal namespaced builder.
     * <p>
     * The alias is converted to lowercase before being stored. If the alias is
     * already registered, an exception is thrown and the previous mapping remains
     * unchanged.
     *
     * @param alias   the alias to associate with the builder
     * @param builder the builder the alias should reference
     * @throws IllegalArgumentException if the alias is already registered
     */
    void registerInternalNamespaceAlias(String alias, NamespacedBuilder<?> builder) {
        NamespacedBuilder<?> previous = this.internalBuilders.put(alias.toLowerCase(), builder);
        if (previous != null) {
            throw new IllegalArgumentException("The alias " + alias +
                    " clashes with the namespace " + previous.getNamespace());
        }
    }

    /**
     * Registers an internal namespaced builder using its own namespace.
     * <p>
     * The namespace is converted to lowercase before being stored. If a mapping
     * with the same key already exists, it is replaced.
     *
     * @param builder the internal builder to register
     */
    void registerInternalNamespace(NamespacedBuilder<?> builder) {
        NamespacedBuilder<?> previous = this.internalBuilders.put(builder.getNamespace(), builder);
        if (previous != null) {
            throw new IllegalArgumentException("The namespace " + builder.getNamespace() +
                    " clashes with the namespace " + previous.getNamespace());
        }
    }

    /**
     * Parses and applies configuration arguments using the default argument pattern.
     * <p>
     * If the argument {@code --argsEqualsSeparated} is present, arguments are parsed
     * using {@link Arguments.Pattern#EQUALS_SEPARATED}. Otherwise,
     * {@link Arguments.Pattern#SPACE_SEPARATED} is used.
     *
     * @param args the command-line arguments to parse
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code args} is {@code null}
     * @see #setFromCli(String[], Arguments.Pattern)
     */
    public @NotNull CraftsNetBuilder setFromCli(@NotNull String... args) {
        boolean equalsSeparated = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("--argsEqualsSeparated")) {
                equalsSeparated = true;
                break;
            }
        }

        return this.setFromCli(
                args,
                equalsSeparated ? Arguments.Pattern.EQUALS_SEPARATED : Arguments.Pattern.SPACE_SEPARATED
        );
    }

    /**
     * Parses and applies command-line arguments using the specified argument pattern.
     * <p>
     * The parsed arguments are applied to this builder through {@link #set(String, Object)}.
     * The special {@code argsEqualsSeparated} argument is ignored after parsing.
     *
     * @param args        the command-line arguments to parse
     * @param argsPattern the pattern used to separate argument names and values
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code args} or {@code argsPattern} is {@code null}
     */
    public @NotNull CraftsNetBuilder setFromCli(@NotNull String[] args, @NotNull Arguments.Pattern argsPattern) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(argsPattern, "argsPattern");

        Arguments arguments = new Arguments(args, argsPattern);
        arguments.getMapView().forEach((arg, value) -> {
            if (arg.equalsIgnoreCase("argsEqualsSeparated")) {
                return;
            }

            this.set(arg, value);
        });
        return this;
    }

    /**
     * Sets a configuration value on this builder or delegates it to an internal
     * namespaced builder.
     * <p>
     * Arguments without a colon are handled by the superclass. Arguments containing
     * a colon are split into a namespace and an argument name. The namespace is
     * resolved case-insensitively against the registered internal builders.
     *
     * @param arg   the configuration argument, optionally prefixed with a namespace
     * @param value the value to assign to the argument
     * @return this builder instance for method chaining
     * @throws NullPointerException     if {@code arg} or {@code value} is {@code null}
     * @throws IllegalArgumentException if the specified namespace is not registered
     */
    @Override
    protected @NotNull CraftsNetBuilder set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");

        if (!arg.contains(":")) {
            return super.set(arg, value);
        }

        String[] parts = arg.split(":", 2);
        String namespace = parts[0];

        NamespacedBuilder<?> builder = this.internalBuilders.get(namespace.toLowerCase());
        if (builder == null) {
            throw new IllegalArgumentException("No namespace " + namespace
                    + " found for argument " + arg);
        }

        builder.set(parts[1], value);
        return this;
    }

    /**
     * Passes the add-on system builder to the supplied consumer.
     *
     * @param builderConsumer the consumer used to configure the add-on system
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code builderConsumer} is {@code null}
     */
    public CraftsNetBuilder addonSystem(Consumer<AddonSystemBuilder> builderConsumer) {
        builderConsumer.accept(this.addonSystemBuilder);
        return this;
    }

    /**
     * Retrieves the add-on system builder.
     *
     * @return the add-on system builder
     */
    public AddonSystemBuilder addonSystem() {
        return this.addonSystemBuilder;
    }

    /**
     * Passes the logging builder to the supplied consumer.
     *
     * @param builderConsumer the consumer used to configure the logging system
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code builderConsumer} is {@code null}
     */
    public CraftsNetBuilder logging(Consumer<LoggingBuilder> builderConsumer) {
        builderConsumer.accept(this.loggingBuilder);
        return this;
    }

    /**
     * Retrieves the logging builder.
     *
     * @return the logging builder
     */
    public LoggingBuilder logging() {
        return this.loggingBuilder;
    }

    /**
     * Passes all registered server builders to the supplied consumer.
     *
     * @param builderConsumer the consumer used to configure each server builder
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code builderConsumer} is {@code null}
     * @see #allServer()
     */
    public CraftsNetBuilder allServer(Consumer<ServerBuilder<?>> builderConsumer) {
        this.allServer().forEach(builderConsumer);
        return this;
    }

    /**
     * Retrieves all registered server builders.
     * <p>
     * The returned collection is produced from the values in the internal builder
     * registry that are instances of {@link ServerBuilder}.
     *
     * @return an unmodifiable list containing the registered server builders
     */
    public Collection<ServerBuilder<?>> allServer() {
        return this.internalBuilders.entrySet().stream()
                .filter(builder -> builder instanceof ServerBuilder)
                .<ServerBuilder<?>>map(entry -> (ServerBuilder<?>) entry.getValue())
                .toList();
    }

    /**
     * Passes the HTTP web server builder to the supplied consumer.
     *
     * @param builderConsumer the consumer used to configure the HTTP web server
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code builderConsumer} is {@code null}
     */
    public CraftsNetBuilder webServer(Consumer<WebServerBuilder> builderConsumer) {
        builderConsumer.accept(this.webServerBuilder);
        return this;
    }

    /**
     * Retrieves the HTTP web server builder.
     *
     * @return the HTTP web server builder
     */
    public WebServerBuilder webServer() {
        return this.webServerBuilder;
    }

    /**
     * Passes the WebSocket server builder to the supplied consumer.
     *
     * @param builderConsumer the consumer used to configure the WebSocket server
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code builderConsumer} is {@code null}
     */
    public CraftsNetBuilder webSocketServer(Consumer<WebSocketServerBuilder> builderConsumer) {
        builderConsumer.accept(this.webSocketServerBuilder);
        return this;
    }

    /**
     * Retrieves the WebSocket server builder.
     *
     * @return the WebSocket server builder
     */
    public WebSocketServerBuilder webSocketServer() {
        return this.webSocketServerBuilder;
    }

    /**
     * Registers a code source for use by CraftsNet.
     * <p>
     * The supplied code source is added to the collection returned by
     * {@link #getCodeSources()}.
     *
     * @param codeSource the code source to register
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code codeSource} is {@code null}
     */
    public CraftsNetBuilder addCodeSource(CodeSource codeSource) {
        this.getCodeSources().add(codeSource);
        return this;
    }

    /**
     * Removes a code source from the registered collection.
     *
     * @param codeSource the code source to remove
     * @return this builder instance for method chaining
     */
    public CraftsNetBuilder removeCodeSource(CodeSource codeSource) {
        this.getCodeSources().remove(codeSource);
        return this;
    }

    /**
     * Retrieves the collection of registered code sources.
     * <p>
     * The returned collection is the underlying collection stored in this builder,
     * so modifications to it directly affect the registered code sources.
     *
     * @return the registered code sources
     * @throws NullPointerException if no code source collection is configured
     */
    public Collection<CodeSource> getCodeSources() {
        return Objects.requireNonNull(
                this.getCollection("codeSources", CodeSource.class),
                "No code sources entry found"
        );
    }

    /**
     * Enables or disables debug mode.
     *
     * @param debug {@code true} to enable debug mode; {@code false} to disable it
     * @return this builder instance for method chaining
     */
    public CraftsNetBuilder debug(boolean debug) {
        return this.set("debug", debug);
    }

    /**
     * Retrieves whether debug mode is enabled.
     *
     * @return {@code true} if debug mode is enabled; otherwise {@code false}
     * @throws NullPointerException if no debug flag is configured
     */
    public boolean debug() {
        return Objects.requireNonNull(
                this.getBoolean("debug"),
                "No debug flag found"
        );
    }

    /**
     * Sets the maximum number of entries allowed in the session cache.
     *
     * @param size the session cache size, which must be non-negative
     * @return this builder instance for method chaining
     */
    public CraftsNetBuilder sessionCacheSize(int size) {
        return this.set("sessionCacheSize", size);
    }

    /**
     * Retrieves the configured session cache size.
     *
     * @return the configured session cache size
     * @throws NullPointerException if no session cache size is configured
     */
    public int sessionCacheSize() {
        return Objects.requireNonNull(
                this.getInt("sessionCacheSize"),
                "No session cache size found"
        );
    }

    /**
     * Configures whether the CraftsNet version check should be skipped.
     *
     * @param skipVersionCheck {@code true} to skip the version check; {@code false} to perform it
     * @return this builder instance for method chaining
     */
    public CraftsNetBuilder skipVersionCheck(boolean skipVersionCheck) {
        return this.set("skipVersionCheck", skipVersionCheck);
    }

    /**
     * Retrieves whether the CraftsNet version check should be skipped.
     *
     * @return {@code true} if the version check should be skipped; otherwise {@code false}
     * @throws NullPointerException if no version-check flag is configured
     */
    public boolean skipVersionCheck() {
        return Objects.requireNonNull(
                this.getBoolean("skipVersionCheck"),
                "No skip version check flag found"
        );
    }

    /**
     * Configures whether temporary files should be stored in the current working directory.
     *
     * @param tempFilesInPwd {@code true} to use the current working directory for temporary
     *                       files; {@code false} otherwise
     * @return this builder instance for method chaining
     */
    public CraftsNetBuilder tempFilesInPwd(boolean tempFilesInPwd) {
        return this.set("tempFilesInPwd", tempFilesInPwd);
    }

    /**
     * Retrieves whether temporary files should be stored in the current working directory.
     *
     * @return {@code true} if temporary files should be stored in the current working
     * directory; otherwise {@code false}
     * @throws NullPointerException if no temporary-file location flag is configured
     */
    public boolean tempFilesInPwd() {
        return Objects.requireNonNull(
                this.getBoolean("tempFilesInPwd"),
                "No temp files in pwd flag found"
        );
    }

    /**
     * Builds and starts a new CraftsNet instance using this builder's configuration.
     *
     * @return the newly created and started CraftsNet instance
     */
    public CraftsNet build() {
        CraftsNet craftsNet = new CraftsNet();
        craftsNet.start(this);
        return craftsNet;
    }

}
