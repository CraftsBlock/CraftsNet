package de.craftsblock.craftsnet.events;

import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Exchange;

/**
 * The RequestEvent class represents an event related to a request. It extends the base Event class and implements the Cancelable interface.
 * Events can be triggered during various stages of request processing. This class provides information about the Exchange and the RouteMapping
 * involved in the request event.
 *
 * @author CraftsBlock
 * @version 1.0
 * @since 1.0.0
 */
public class RequestEvent extends Event implements Cancelable {

    private final Exchange exchange;

    private final RouteRegistry.RouteMapping mapping;
    private boolean cancelled = false;

    /**
     * Constructs a new RequestEvent with the specified Exchange and RouteMapping.
     *
     * @param exchange The Exchange object representing the request and its associated data.
     * @param mapping  The RouteMapping object associated with the request, can be null if not applicable.
     */
    public RequestEvent(Exchange exchange, RouteRegistry.RouteMapping mapping) {
        this.exchange = exchange;
        this.mapping = mapping;
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
     * Gets the RouteMapping associated with the request event.
     *
     * @return The RouteMapping object representing the mapping for the request route.
     */
    public RouteRegistry.RouteMapping getMapping() {
        return mapping;
    }

    /**
     * Checks if the request event has a valid RouteMapping associated with it.
     *
     * @return true if the event has a valid RouteMapping, false otherwise.
     */
    public boolean hasMapping() {
        return mapping != null;
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
