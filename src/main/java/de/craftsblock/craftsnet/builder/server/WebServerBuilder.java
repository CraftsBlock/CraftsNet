package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.AbstractCraftsNetBuilder;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

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

        this.backlog(0);
        this.maxUriLength(4096);
        this.port(5000);
        this.responseEncoding(false);
        this.skipDefaultRoute(false);
        this.ssl(false);
        this.state(ServerState.DYNAMIC);
    }

}
