package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

import java.util.List;

/**
 * The ClientConnectEvent class represents an event related to a client connection to a socket.
 * It extends the base {@link CancellableEvent} to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since 2.1.1-SNAPSHOT
 */
public class ClientConnectEvent extends CancellableEvent {

    private final SocketExchange exchange;
    private final List<RouteRegistry.EndpointMapping> mappings;
    private String reason;

    /**
     * Constructs a new ClientConnectEvent with the specified SocketExchange and SocketMapping.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param mappings A list of SocketMapping objects associated with the client connection event.
     */
    public ClientConnectEvent(SocketExchange exchange, List<RouteRegistry.EndpointMapping> mappings) {
        this.exchange = exchange;
        this.mappings = mappings;
    }

    /**
     * Gets the SocketExchange object associated with the client connection event.
     *
     * @return The SocketExchange object representing the socket connection and its associated data.
     */
    public SocketExchange getExchange() {
        return exchange;
    }

    /**
     * Gets a list of all SocketMappings associated with the client connection event.
     *
     * @return A list of SocketMapping objects representing the mapping for the socket connection.
     */
    public List<RouteRegistry.EndpointMapping> getMappings() {
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
     * Sets the reason for the client connection event.
     *
     * @param reason The reason for the client connection.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Gets the reason for the client connection event.
     *
     * @return The reason for the client connection.
     */
    public String getReason() {
        return reason;
    }

}
