package de.craftsblock.craftsnet.events.requests;

import de.craftsblock.craftscore.event.Cancellable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.http.Exchange;

/**
 * This event is triggered before a http request is processed as route or share.
 * It allows for pre-processing of the request, including the ability to cancel it before it is handled.
 * The event holds an {@link Exchange} object that contains the details of the request.
 * By implementing {@link Cancellable}, this event can be cancelled, stopping further processing.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class PreRequestEvent extends Event implements Cancellable {

    private final Exchange exchange;

    private boolean cancelled = false;

    /**
     * Constructs a new {@code PreRequestEvent} with the provided {@link Exchange} object.
     *
     * @param exchange the {@link Exchange} object containing the HTTP request data.
     */
    public PreRequestEvent(Exchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Gets the Exchange object associated with the request event.
     *
     * @return The Exchange object representing the request and its associated data.
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Sets the cancelled flag for the event, indicating whether the event is cancelled or not.
     *
     * @param cancelled true to cancel the event, false to allow processing.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Checks if the event has been cancelled.
     *
     * @return true if the event is cancelled, false otherwise.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }


}
