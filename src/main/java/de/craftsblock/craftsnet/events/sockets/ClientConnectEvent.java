package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

/**
 * The ClientConnectEvent class represents an event related to a client connection to a socket.
 * It extends the base Event class and implements the Cancelable interface to support event cancellation.
 *
 * @author CraftsBlock
 * @since 2.1.1
 */
public class ClientConnectEvent extends Event implements Cancelable {

    private final SocketExchange exchange;
    private final RouteRegistry.SocketMapping mapping;
    private boolean cancelled = false;
    private String reason;

    /**
     * Constructs a new ClientConnectEvent with the specified SocketExchange and SocketMapping.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param mapping  The SocketMapping object associated with the client connection event.
     */
    public ClientConnectEvent(SocketExchange exchange, RouteRegistry.SocketMapping mapping) {
        this.exchange = exchange;
        this.mapping = mapping;
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
     * Gets the SocketMapping associated with the client connection event.
     *
     * @return The SocketMapping object representing the mapping for the socket connection.
     */
    public RouteRegistry.SocketMapping getMapping() {
        return mapping;
    }

    /**
     * Checks if the client connection event has a valid SocketMapping associated with it.
     *
     * @return true if the event has a valid SocketMapping, false otherwise.
     */
    public boolean hasMapping() {
        return mapping != null;
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
