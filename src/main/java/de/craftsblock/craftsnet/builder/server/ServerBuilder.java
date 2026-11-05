package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for configuring an individual server instance.
 * <p>
 * This builder extends {@link NamespacedBuilder} and provides type-safe methods for configuring
 * server-specific settings, such as the connection backlog, maximum URI length, listening port,
 * response encoding, SSL, server state, and trusted proxy headers.
 * <p>
 * Each configuration value is stored under its corresponding argument name in the inherited
 * namespaced configuration.
 *
 * @param <T> the concrete builder type, used to preserve the fluent API return type
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see NamespacedBuilder
 * @see ServerState
 * @see ArgumentTransformer
 * @since 4.0.0
 */
public class ServerBuilder<T extends ServerBuilder<T>> extends NamespacedBuilder<T> {

    private final ServerTrustBuilder trustBuilder;

    /**
     * Creates a new server builder for the specified root builder and namespace.
     * <p>
     * During initialization, the supported server arguments and their corresponding
     * transformation rules are registered.
     *
     * @param root      the root CraftsNet builder this server builder belongs to
     * @param namespace the namespace used to identify this server's configuration
     */
    public ServerBuilder(@NotNull CraftsNetBuilder root, String namespace) {
        super(root, namespace);

        this.trustBuilder = new ServerTrustBuilder(root);

        this.addKnownArgument("backlog", ArgumentTransformer.integerNumber(0, Integer.MAX_VALUE));
        this.addKnownArgument("maxHeaders", ArgumentTransformer.integerNumber(0, Integer.MAX_VALUE));
        this.addKnownArgument("maxHeaderSize", ArgumentTransformer.integerNumber(1, Integer.MAX_VALUE));
        this.addKnownArgument("maxUriLength", ArgumentTransformer.integerNumber(1, Integer.MAX_VALUE));
        this.addKnownArgument("port", ArgumentTransformer.integerNumber(0, 65365));
        this.addKnownArgument("responseEncoding", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("skipDefaultRoute", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("ssl", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("state", ArgumentTransformer.enumType(ServerState.class));
    }

    /**
     * {@inheritDoc}
     *
     * @param arg   {@inheritDoc}
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected @NotNull T set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");

        if (!arg.contains(":")) {
            return super.set(arg, value);
        }

        String[] parts = arg.split(":", 2);
        if (parts[0].equalsIgnoreCase("trust")) {
            this.trustBuilder.set(parts[1], value);
            return getOwner();
        }

        throw new IllegalArgumentException("Unknown namespace " + parts[0] + " for argument " + arg);
    }

    /**
     * Sets the maximum number of connections that may wait in the server's connection queue.
     *
     * @param backlog the connection backlog, between {@code 0} and {@link Integer#MAX_VALUE}
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the backlog is outside the supported range
     */
    public @NotNull T backlog(@Range(from = 0, to = Integer.MAX_VALUE) int backlog) {
        ArgumentTransformer.validatedNumber(backlog, 0, Integer.MAX_VALUE);
        return this.set("backlog", backlog);
    }

    /**
     * Retrieves the configured connection backlog.
     *
     * @return the configured connection backlog
     * @throws NullPointerException if no backlog value is configured
     */
    public int backlog() {
        return Objects.requireNonNull(
                this.getInt("backlog"),
                "No backlog found for the " + getNamespace()
        );
    }

    /**
     * Sets the maximum permitted header amount.
     *
     * @param maxHeaders the maximum header amount, greater than or equal to {@code 1}
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the value is outside the supported range
     */
    public @NotNull T maxHeaders(@Range(from = 1, to = Integer.MAX_VALUE) int maxHeaders) {
        ArgumentTransformer.validatedNumber(maxHeaders, 1, Integer.MAX_VALUE);
        return this.set("maxHeaders", maxHeaders);
    }

    /**
     * Retrieves the configured maximum header amount.
     *
     * @return the configured maximum header amount
     * @throws NullPointerException if no maximum header amount is configured
     */
    public int maxHeaders() {
        return Objects.requireNonNull(
                this.getInt("maxHeaders"),
                "No max headers found for the " + getNamespace()
        );
    }

    /**
     * Sets the maximum permitted header size.
     *
     * @param maxHeaderSize the maximum header size, greater than or equal to {@code 1}
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the value is outside the supported range
     */
    public @NotNull T maxHeaderSize(@Range(from = 1, to = Integer.MAX_VALUE) int maxHeaderSize) {
        ArgumentTransformer.validatedNumber(maxHeaderSize, 1, Integer.MAX_VALUE);
        return this.set("maxHeaders", maxHeaderSize);
    }

    /**
     * Retrieves the configured maximum header size.
     *
     * @return the configured maximum header size
     * @throws NullPointerException if no maximum header size is configured
     */
    public int maxHeaderSize() {
        return Objects.requireNonNull(
                this.getInt("maxHeaderSize"),
                "No max header size found for the " + getNamespace()
        );
    }

    /**
     * Sets the maximum permitted length of a request URI.
     *
     * @param maxUriLength the maximum URI length, greater than or equal to {@code 1}
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the value is outside the supported range
     */
    public @NotNull T maxUriLength(@Range(from = 1, to = Integer.MAX_VALUE) int maxUriLength) {
        ArgumentTransformer.validatedNumber(maxUriLength, 1, Integer.MAX_VALUE);
        return this.set("maxUriLength", maxUriLength);
    }

    /**
     * Retrieves the configured maximum URI length.
     *
     * @return the configured maximum URI length
     * @throws NullPointerException if no maximum URI length is configured
     */
    public int maxUriLength() {
        return Objects.requireNonNull(
                this.getInt("maxUriLength"),
                "No max uri length found for the " + getNamespace()
        );
    }

    /**
     * Sets the port used by the server.
     *
     * @param port the server port, between {@code 0} and {@code 65365}
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the port is outside the supported range
     */
    public @NotNull T port(int port) {
        ArgumentTransformer.validatedNumber(port, 0, 65365);
        return this.set("port", port);
    }

    /**
     * Retrieves the configured server port.
     *
     * @return the configured server port
     * @throws NullPointerException if no port is configured
     */
    public int port() {
        return Objects.requireNonNull(
                this.getInt("port"),
                "No port found for the " + getNamespace()
        );
    }

    /**
     * Enables or disables response encoding.
     *
     * @param responseEncoding {@code true} to enable response encoding; {@code false} to disable it
     * @return this builder instance for method chaining
     */
    public @NotNull T responseEncoding(boolean responseEncoding) {
        return this.set("responseEncoding", responseEncoding);
    }

    /**
     * Retrieves whether response encoding is enabled.
     *
     * @return {@code true} if response encoding is enabled; otherwise {@code false}
     * @throws NullPointerException if no response encoding flag is configured
     */
    public boolean responseEncoding() {
        return Objects.requireNonNull(
                this.getBoolean("responseEncoding"),
                "No response encoding flag found for " + this.getNamespace()
        );
    }

    /**
     * Configures whether the server should skip registration of its default route.
     *
     * @param skipDefaultRoute {@code true} to skip the default route; {@code false} otherwise
     * @return this builder instance for method chaining
     */
    public @NotNull T skipDefaultRoute(boolean skipDefaultRoute) {
        return this.set("skipDefaultRoute", skipDefaultRoute);
    }

    /**
     * Retrieves whether registration of the default route should be skipped.
     *
     * @return {@code true} if the default route should be skipped; otherwise {@code false}
     * @throws NullPointerException if no skip-default-route flag is configured
     */
    public boolean skipDefaultRoute() {
        return Objects.requireNonNull(
                this.getBoolean("skipDefaultRoute"),
                "No skip default route state found for " + getNamespace()
        );
    }

    /**
     * Enables or disables SSL for the server.
     *
     * @param ssl {@code true} to enable SSL; {@code false} to disable it
     * @return this builder instance for method chaining
     */
    public @NotNull T ssl(boolean ssl) {
        return this.set("ssl", ssl);
    }

    /**
     * Retrieves whether SSL is enabled for the server.
     *
     * @return {@code true} if SSL is enabled; otherwise {@code false}
     * @throws NullPointerException if no SSL state is configured
     */
    public boolean ssl() {
        return Objects.requireNonNull(
                this.getBoolean("ssl"),
                "No ssl state found for " + getNamespace()
        );
    }

    /**
     * Sets the state in which the server should operate.
     *
     * @param state the server state to configure
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public @NotNull T state(@NotNull ServerState state) {
        return this.set("state", state);
    }

    /**
     * Retrieves the configured server state.
     *
     * @return the configured {@link ServerState}
     * @throws NullPointerException if no server state is configured
     * @see ServerState
     */
    public @NotNull ServerState state() {
        return Objects.requireNonNull(
                this.getEnum("state", ServerState.class),
                "No state found for " + getNamespace()
        );
    }

    /**
     * Checks whether the server is configured with the specified state.
     *
     * @param state the state to compare with the configured server state
     * @return {@code true} if the configured state is identical to the specified state;
     * otherwise {@code false}
     * @throws NullPointerException if {@code state} is {@code null} or no server state is configured
     */
    public boolean isState(@NotNull ServerState state) {
        return this.state() == state;
    }

    /**
     * Returns the server trust builder associated with this builder.
     *
     * @return the server trust builder
     */
    public @NotNull ServerTrustBuilder trust() {
        return this.trustBuilder;
    }

    /**
     * Applies the given configuration consumer to the server trust builder.
     * <p>
     * This method allows configuring server trust settings directly within a
     * consumer while retaining the fluent API of the owning builder.
     *
     * @param builderConsumer the consumer used to configure the server trust builder
     * @return the owner builder for method chaining
     */
    public @NotNull T trust(Consumer<ServerTrustBuilder> builderConsumer) {
        builderConsumer.accept(this.trustBuilder);
        return this.getOwner();
    }

}
