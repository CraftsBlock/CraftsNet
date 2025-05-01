package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.events.EventWithCancelReason;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;

/**
 * The ClientConnectEvent class represents an event related to a client connection to a socket.
 * It extends the base {@link EventWithCancelReason} to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.0
 * @see EventWithCancelReason
 * @see GenericSocketEventBase
 * @since 2.1.1-SNAPSHOT
 */
public class ClientConnectEvent extends EventWithCancelReason implements GenericSocketEventBase {

    private final SocketExchange exchange;
    private final EnumMap<ProcessPriority.Priority, List<RouteRegistry.EndpointMapping>> mappings;

    private boolean allowWithoutMapping = false;

    /**
     * Constructs a new ClientConnectEvent with the specified SocketExchange and SocketMapping.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     */
    public ClientConnectEvent(SocketExchange exchange) {
        this.exchange = exchange;
        this.mappings = exchange.client().getEndpoint();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull SocketExchange getExchange() {
        return exchange;
    }

    /**
     * Gets a {@link EnumMap} of all {@link RouteRegistry.EndpointMapping} associated with the client connection event.
     *
     * @return A {@link EnumMap} of {@link RouteRegistry.EndpointMapping} objects representing the mapping for the socket connection.
     */
    public EnumMap<ProcessPriority.Priority, List<RouteRegistry.EndpointMapping>> getMappings() {
        return mappings;
    }

    /**
     * Checks if the client connection event has at least one valid SocketMapping associated with it.
     *
     * @return true if the event has at least one valid SocketMapping, false otherwise.
     */
    public boolean hasMappings() {
        return mappings != null && !mappings.isEmpty();
    }

    /**
     * Sets whether the websocket client is allowed to connect to the server without a valid endpoint or not.
     *
     * @param allowWithoutMapping {@code true} if the client is allowed to connect without a valid endpoint, {@code false} otherwise.
     */
    public void allowWithoutMapping(boolean allowWithoutMapping) {
        this.allowWithoutMapping = allowWithoutMapping;
    }

    /**
     * Retrieves whether the websocket client is allowed to connect without a valid endpoint or not.
     *
     * @return {@code true} if the client is allowed to connect without a valid endpoint, {@code false} otherwise.
     */
    public boolean isAllowedWithoutMapping() {
        return allowWithoutMapping;
    }
}
