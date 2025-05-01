package de.craftsblock.craftsnet.events.requests;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.http.Exchange;
import org.jetbrains.annotations.NotNull;

/**
 * This event is triggered before a http request is processed as route or share.
 * It allows for pre-processing of the request, including the ability to cancel it before it is handled.
 * The event holds an {@link Exchange} object that contains the details of the request.
 * By extending {@link CancellableEvent}, this event can be cancelled, stopping further processing.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.0
 * @see RequestEventBase
 * @since 3.1.0-SNAPSHOT
 */
public class PreRequestEvent extends CancellableEvent implements RequestEventBase {

    private final Exchange exchange;

    /**
     * Constructs a new {@code PreRequestEvent} with the provided {@link Exchange} object.
     *
     * @param exchange the {@link Exchange} object containing the HTTP request data.
     */
    public PreRequestEvent(Exchange exchange) {
        this.exchange = exchange;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull Exchange getExchange() {
        return exchange;
    }

}
