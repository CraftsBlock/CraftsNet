package de.craftsblock.craftsnet.events;

import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Exchange;

import java.util.List;

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

    private final List<RouteRegistry.RouteMapping> mappings;
    private boolean cancelled = false;

    /**
     * Constructs a new RequestEvent with the specified Exchange and RouteMappings.
     *
     * @param exchange The Exchange object representing the request and its associated data.
     * @param mappings  The RouteMapping objects associated with the request, can be null if not applicable.
     */
    public RequestEvent(Exchange exchange, List<RouteRegistry.RouteMapping> mappings) {
        this.exchange = exchange;
        this.mappings = mappings;
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
     * Get the RouteMappings associated with the request event.
     *
     * @return The RouteMapping objects representing the mapping for the request route.
     */
    public List<RouteRegistry.RouteMapping> getMappings() {
        return mappings;
    }

    /**
     * Checks if the request event has valid RouteMappings associated with it.
     *
     * @return true if the event has valid RouteMappings, false otherwise.
     */
    public boolean hasMappings() {
        return mappings != null && !mappings.isEmpty();
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
