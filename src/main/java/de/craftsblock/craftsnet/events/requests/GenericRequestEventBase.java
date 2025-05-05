package de.craftsblock.craftsnet.events.requests;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the base for all request events.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see Exchange
 * @see Request
 * @see Response
 * @since 3.3.6-SNAPSHOT
 */
public interface GenericRequestEventBase {

    /**
     * Gets the {@link Exchange} which stores the involved request, response and some other data.
     *
     * @return The {@link Exchange}.
     */
    @NotNull
    Exchange getExchange();

    /**
     * Gets the {@link Request} which is involved in this event.
     *
     * @return The {@link Request}
     * @since 3.3.6-SNAPSHOT
     */
    default @NotNull Request getRequest() {
        return getExchange().request();
    }

    /**
     * Gets the {@link Response} which is involved in this event.
     *
     * @return The {@link Response}
     * @since 3.3.6-SNAPSHOT
     */
    default @NotNull Response getResponse() {
        return getExchange().response();
    }

}
