package de.craftsblock.craftsnet.events.requests.routes;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.events.EventWithCancelReason;
import de.craftsblock.craftsnet.events.requests.GenericRequestEventBase;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * The {@link RouteRequestEvent} class represents an event related to a route request.
 * This class provides information about the {@link HttpExchange} and the {@link RouteRegistry.EndpointMapping}
 * involved in the request event.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see EventWithCancelReason
 * @see GenericRequestEventBase
 * @since 1.0.0-SNAPSHOT
 */
public class RouteRequestEvent extends EventWithCancelReason implements GenericRequestEventBase {

    private final HttpExchange httpExchange;

    /**
     * Constructs a new {@link RouteRequestEvent} with the specified {@link HttpExchange} and {@link RouteRegistry.EndpointMapping}.
     *
     * @param httpExchange The {@link HttpExchange} object representing the request and its associated data.
     */
    public RouteRequestEvent(HttpExchange httpExchange) {
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

    /**
     * Get a list of {@link RouteRegistry.EndpointMapping} associated with the request event.
     *
     * @return A list of {@link RouteRegistry.EndpointMapping} representing the mappings for the request route.
     */
    public Collection<RouteRegistry.EndpointMapping> getMappings() {
        return getRequest().getRoutes();
    }

    /**
     * Checks if the request event has at least one valid {@link RouteRegistry.EndpointMapping} associated with it.
     *
     * @return true if the event has at least one valid {@link RouteRegistry.EndpointMapping}, false otherwise.
     */
    public boolean hasMappings() {
        var mappings = getMappings();
        return mappings != null && !mappings.isEmpty();
    }

}
