package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Builder for configuring the trusted proxy and forwarded-header settings of a server.
 * <p>
 * This builder manages three independent lists:
 * <ul>
 *     <li>Headers used to determine the forwarded host.</li>
 *     <li>Headers used to determine the forwarded client IP address.</li>
 *     <li>Proxy addresses or identifiers that are considered trusted.</li>
 * </ul>
 * <p>
 * The builder is registered under the {@code trust} namespace and supports
 * configuring its values through the namespaced argument system.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see ServerTrust
 * @see CraftsNetBuilder
 * @since 4.0.0
 */
public class ServerTrustBuilder extends NamespacedBuilder<ServerTrustBuilder> {

    /**
     * Creates a new server trust builder and associates it with the given root builder.
     * <p>
     * The builder registers its supported arguments and initializes all configuration
     * lists as empty lists.
     *
     * @param root the root CraftsNet builder this builder belongs to
     */
    public ServerTrustBuilder(@NotNull CraftsNetBuilder root) {
        super(root, "trust", false, false);
        this.acquire(this);

        this.addKnownArgument("forwardedHostHeaders", ArgumentTransformer.list());
        this.addKnownArgument("forwardedIpHeaders", ArgumentTransformer.list());
        this.addKnownArgument("trustedProxies", ArgumentTransformer.list());

        this.forwardedHostHeaders(Collections.emptyList());
        this.forwardedIpHeaders(Collections.emptyList());
        this.trustedProxies(Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overwritten so {@link ServerBuilder} can access it.
     *
     * @param arg   {@inheritDoc}
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected @NotNull ServerTrustBuilder set(@NotNull String arg, @NotNull Object value) {
        return super.set(arg, value);
    }

    /**
     * Sets the HTTP headers used to retrieve the original host from forwarded requests.
     *
     * @param headers the header names to trust, in lookup order
     * @return this builder for method chaining
     */
    public ServerTrustBuilder forwardedHostHeaders(String... headers) {
        return this.forwardedHostHeaders(List.of(headers));
    }

    /**
     * Sets the HTTP headers used to retrieve the original host from forwarded requests.
     *
     * @param headers the header names to trust, in lookup order
     * @return this builder for method chaining
     */
    public ServerTrustBuilder forwardedHostHeaders(List<String> headers) {
        return this.set("forwardedHostHeaders", headers);
    }

    /**
     * Returns the configured HTTP headers used to retrieve the original host.
     *
     * @return the configured forwarded-host header names
     * @throws NullPointerException if no value is stored for the forwarded-host headers
     */
    public List<String> forwardedHostHeaders() {
        return (List<String>) Objects.requireNonNull(
                this.getCollection("forwardedHostHeaders", String.class),
                "No forwarded host headers found"
        );
    }

    /**
     * Sets the HTTP headers used to retrieve the original client IP address from forwarded requests.
     *
     * @param headers the header names to trust, in lookup order
     * @return this builder for method chaining
     */
    public ServerTrustBuilder forwardedIpHeaders(String... headers) {
        return this.forwardedIpHeaders(List.of(headers));
    }

    /**
     * Sets the HTTP headers used to retrieve the original client IP address from forwarded requests.
     *
     * @param headers the header names to trust, in lookup order
     * @return this builder for method chaining
     */
    public ServerTrustBuilder forwardedIpHeaders(List<String> headers) {
        return this.set("forwardedIpHeaders", headers);
    }

    /**
     * Returns the configured HTTP headers used to retrieve the original client IP address.
     *
     * @return the configured forwarded-IP header names
     * @throws NullPointerException if no value is stored for the forwarded-IP headers
     */
    public List<String> forwardedIpHeaders() {
        return (List<String>) Objects.requireNonNull(
                this.getCollection("forwardedIpHeaders", String.class),
                "No forwarded ip headers found"
        );
    }

    /**
     * Sets the proxies that are considered trusted when processing forwarded request information.
     *
     * @param proxies the proxy addresses or identifiers to trust
     * @return this builder for method chaining
     */
    public ServerTrustBuilder trustedProxies(String... proxies) {
        return this.trustedProxies(List.of(proxies));
    }

    /**
     * Sets the proxies that are considered trusted when processing forwarded request information.
     *
     * @param proxies the proxy addresses or identifiers to trust
     * @return this builder for method chaining
     */
    public ServerTrustBuilder trustedProxies(List<String> proxies) {
        return this.set("trustedProxies", proxies);
    }

    /**
     * Returns the configured trusted proxies.
     *
     * @return the configured trusted proxy addresses or identifiers
     * @throws NullPointerException if no value is stored for the trusted proxies
     */
    public List<String> trustedProxies() {
        return (List<String>) Objects.requireNonNull(
                this.getCollection("trustedProxies", String.class),
                "No trusted proxies found"
        );
    }

    /**
     * Builds a {@link ServerTrust} instance using the currently configured header lists
     * and trusted proxies.
     *
     * @return a new server trust configuration
     * @throws NullPointerException if any of the required configuration lists is absent
     */
    public ServerTrust build() {
        return new ServerTrust(
                forwardedHostHeaders(),
                forwardedIpHeaders(),
                trustedProxies()
        );
    }

}