package de.craftsblock.craftsnet.api;

import de.craftsblock.craftsnet.api.utils.ProtocolVersion;
import de.craftsblock.craftsnet.api.utils.Scheme;

/**
 * Acts as the base for all exchanges that exists.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.3.0
 * @since 1.0.0-SNAPSHOT
 */
public interface BaseExchange extends AutoCloseable {

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
     * Gets the {@link ProtocolVersion} of the exchange.
     *
     * @return The {@link ProtocolVersion} of the exchange.
     * @since 3.3.2-SNAPSHOT
     */
    ProtocolVersion protocolVersion();

}
