package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.AbstractCraftsNetBuilder;
import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;

/**
 * Builder for configuring the default web server.
 * <p>
 * This class specializes {@link ServerBuilder} for the web server by assigning it the
 * {@code web} namespace and initializing its default configuration values.
 * <p>
 * The builder also registers itself with the root {@link CraftsNetBuilder} through
 * {@link #acquire(AbstractCraftsNetBuilder)}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @see ServerBuilder
 * @see ServerState
 * @since 4.0.0
 */
public class WebServerBuilder extends ServerBuilder<WebServerBuilder> {

    /**
     * Creates and initializes the web server builder.
     * <p>
     * The constructor assigns the {@code web} namespace, registers this builder with the
     * root builder, and applies the default web server configuration.
     *
     * @param root the root CraftsNet builder this web server builder belongs to
     */
    public WebServerBuilder(@NotNull CraftsNetBuilder root) {
        super(root, "web");
        this.acquire(this);

        this.addKnownArgument("maxBodySize", ArgumentTransformer.longNumber(1, Long.MAX_VALUE));

        this.maxBodySize(10 * 1024 * 1024);

        this.backlog(128);
        this.maxHeaders(100);
        this.maxHeaderSize(32768);
        this.maxUriLength(4096);
        this.port(5000);
        this.responseEncoding(false);
        this.skipDefaultRoute(false);
        this.ssl(false);
        this.state(ServerState.DYNAMIC);

        this.corePoolSize(16);
        this.maximumPoolSize(64);
        this.keepAliveTime(60);
        this.queueCapacity(128);
    }

    /**
     * Sets the maximum permitted size of an HTTP request body.
     * <p>
     * The specified value must be greater than or equal to {@code 1}.
     *
     * @param maxBodySize the maximum request body size in bytes
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the value is less than {@code 1}
     */
    public WebServerBuilder maxBodySize(@Range(from = 1, to = Long.MAX_VALUE) long maxBodySize) {
        ArgumentTransformer.validatedNumber(maxBodySize, 1, Long.MAX_VALUE);
        return this.set("maxBodySize", maxBodySize);
    }

    /**
     * Returns the configured maximum permitted HTTP request body size.
     *
     * @return the maximum request body size in bytes
     * @throws NullPointerException if no maximum body size is configured
     */
    public long maxBodySize() {
        return Objects.requireNonNull(
                this.getLong("maxBodySize"),
                "No max body size found for the web server"
        );
    }

}
