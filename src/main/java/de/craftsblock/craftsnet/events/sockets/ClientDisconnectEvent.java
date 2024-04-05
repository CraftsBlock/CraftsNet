package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

/**
 * The ClientDisconnectEvent class represents an event related to a client disconnection from a websocket connection.
 * It extends the base Event class and provides information about the SocketExchange and the SocketMapping associated with the disconnection event.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since 2.1.1
 */
public class ClientDisconnectEvent extends Event {

    private final SocketExchange exchange;
    private final RouteRegistry.SocketMapping mapping;

    /**
     * Constructs a new ClientDisconnectEvent with the specified SocketExchange and SocketMapping.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param mapping  The SocketMapping object associated with the client disconnection event.
     */
    public ClientDisconnectEvent(SocketExchange exchange, RouteRegistry.SocketMapping mapping) {
        this.exchange = exchange;
        this.mapping = mapping;
    }

    /**
     * Gets the SocketMapping associated with the client disconnection event.
     *
     * @return The SocketMapping object representing the mapping for the socket connection.
     */
    public RouteRegistry.SocketMapping getMapping() {
        return mapping;
    }

    /**
     * Checks if the client disconnection event has a valid SocketMapping associated with it.
     *
     * @return true if the event has a valid SocketMapping, false otherwise.
     */
    public boolean hasMapping() {
        return mapping != null;
    }

    /**
     * Gets the SocketExchange object associated with the client disconnection event.
     *
     * @return The SocketExchange object representing the socket connection and its associated data.
     */
    public SocketExchange getExchange() {
        return exchange;
    }

}
