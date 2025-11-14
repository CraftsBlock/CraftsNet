package de.craftsblock.craftsnet.api;

import de.craftsblock.craftsnet.api.session.Session;
import de.craftsblock.craftsnet.api.utils.Context;
import de.craftsblock.craftsnet.api.utils.ProtocolVersion;
import de.craftsblock.craftsnet.api.utils.Scheme;

/**
 * Acts as the base for all exchanges that exists.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.5.0
 * @since 1.0.0-SNAPSHOT
 */
public interface BaseExchange extends AutoCloseable {

    /**
     * Performs last actions before the exchange is closed.
     */
    @Override
    default void close() {
        Context context = context();
        if (context != null) context.clear();
    }

    /**
     * Get the {@link Context} of the exchange.
     *
     * @return The {@link Context} of the exchange.
     * @since 3.5.6
     */
    Context context();

    /**
     * Gets the {@link ProtocolVersion} of the exchange.
     *
     * @return The {@link ProtocolVersion} of the exchange.
     * @since 3.3.2-SNAPSHOT
     */
    ProtocolVersion protocolVersion();

    /**
     * Gets the {@link Scheme} of the exchange.
     *
     * @return The {@link Scheme} of the exchange.
     * @since 3.3.2-SNAPSHOT
     */
    default Scheme scheme() {
        return protocolVersion().scheme();
    }

    /**
     * Gets the {@link Session} of the exchange.
     *
     * @return The {@link Session} of the exchange.
     * @since 3.5.6
     */
    Session session();
}
