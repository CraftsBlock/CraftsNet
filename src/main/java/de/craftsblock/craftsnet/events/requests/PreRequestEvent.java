package de.craftsblock.craftsnet.events.requests;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import org.jetbrains.annotations.NotNull;

/**
 * This event is triggered before a http request is processed as route or share.
 * It allows for pre-processing of the request, including the ability to cancel it before it is handled.
 * The event holds an {@link HttpExchange} object that contains the details of the request.
 * By extending {@link CancellableEvent}, this event can be cancelled, stopping further processing.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see GenericRequestEventBase
 * @since 3.1.0-SNAPSHOT
 */
public class PreRequestEvent extends CancellableEvent implements GenericRequestEventBase {

    private final HttpExchange httpExchange;

    /**
     * Constructs a new {@code PreRequestEvent} with the provided {@link HttpExchange} object.
     *
     * @param httpExchange the {@link HttpExchange} object containing the HTTP request data.
     */
    public PreRequestEvent(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean isAsyncAllowed() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull HttpExchange getExchange() {
        return httpExchange;
    }

}
