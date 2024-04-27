package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

import java.util.List;

/**
 * The ClientDisconnectEvent class represents an event related to a client disconnection from a websocket connection.
 * It extends the base Event class and provides information about the SocketExchange and the SocketMapping associated with the disconnection event.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since CraftsNet-2.1.1
 */
public class ClientDisconnectEvent extends Event {

    private final SocketExchange exchange;
    private final List<RouteRegistry.SocketMapping> mappings;

    /**
     * Constructs a new ClientDisconnectEvent with the specified SocketExchange and SocketMapping.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param mappings A list of all SocketMapping objects associated with the client disconnection event.
     */
    public ClientDisconnectEvent(SocketExchange exchange, List<RouteRegistry.SocketMapping> mappings) {
        this.exchange = exchange;
        this.mappings = mappings;
    }

    /**
     * Gets a list of all SocketMappings associated with the client disconnection event.
     *
     * @return A list of all SocketMapping objects representing the mapping for the socket connection.
     */
    public List<RouteRegistry.SocketMapping> getMappings() {
        return mappings;
    }

    /**
     * Checks if the client disconnection event has at least one valid SocketMapping associated with it.
     *
     * @return true if the event has at least one valid SocketMapping, false otherwise.
     */
    public boolean hasMappings() {
        return mappings != null && !mappings.isEmpty();
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
