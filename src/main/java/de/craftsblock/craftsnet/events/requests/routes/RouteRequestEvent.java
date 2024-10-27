package de.craftsblock.craftsnet.events.requests.routes;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Exchange;

import java.util.List;

/**
 * The {@link RouteRequestEvent} class represents an event related to a route request.
 * This class provides information about the {@link Exchange} and the {@link RouteRegistry.EndpointMapping}
 * involved in the request event.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @since 1.0.0-SNAPSHOT
 */
public class RouteRequestEvent extends CancellableEvent {

    private final Exchange exchange;

    private final List<RouteRegistry.EndpointMapping> mappings;

    private String cancelReason;

    /**
     * Constructs a new {@link RouteRequestEvent} with the specified {@link Exchange} and {@link RouteRegistry.EndpointMapping}.
     *
     * @param exchange The {@link Exchange} object representing the request and its associated data.
     * @param mappings The {@link RouteRegistry.EndpointMapping} objects associated with the request, can be null if not applicable.
     */
    public RouteRequestEvent(Exchange exchange, List<RouteRegistry.EndpointMapping> mappings) {
        this.exchange = exchange;
        this.mappings = mappings;
    }

    /**
     * Gets the {@link Exchange} object associated with the request event.
     *
     * @return The {@link Exchange} object representing the request and its associated data.
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Get a list of {@link RouteRegistry.EndpointMapping} associated with the request event.
     *
     * @return A list of {@link RouteRegistry.EndpointMapping} representing the mappings for the request route.
     */
    public List<RouteRegistry.EndpointMapping> getMappings() {
        return mappings;
    }

    /**
     * Checks if the request event has at least one valid {@link RouteRegistry.EndpointMapping} associated with it.
     *
     * @return true if the event has at least one valid {@link RouteRegistry.EndpointMapping}, false otherwise.
     */
    public boolean hasMappings() {
        return mappings != null && !mappings.isEmpty();
    }

    /**
     * Sets a custom cancel reason which is printed to the console
     *
     * @param cancelReason The cancel reason which is printed to the console
     */
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /**
     * Gets the custom cancel reason which was set by one of the listeners.
     *
     * @return The cancel reason
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * Checks and returns whether a custom cancel reason was set by one of the listeners.
     *
     * @return true if a custom cancel reason was set, false otherwise.
     */
    public boolean hasCancelReason() {
        return this.cancelReason != null;
    }

}
