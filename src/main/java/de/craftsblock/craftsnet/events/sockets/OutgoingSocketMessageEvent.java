package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

/**
 * The OutgoingSocketMessageEvent class represents an event related to an outgoing message on a websocket connection.
 * It extends the base Event class and implements the Cancelable interface to support event cancellation.
 *
 * @author CraftsBlock
 * @version 1.0
 * @since 2.1.1
 */
public class OutgoingSocketMessageEvent extends Event implements Cancelable {

    private final SocketExchange exchange;
    private boolean cancelled = false;

    private String data;

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param data     The outgoing message data for this event.
     */
    public OutgoingSocketMessageEvent(SocketExchange exchange, String data) {
        this.exchange = exchange;
        this.data = data;
    }

    /**
     * Gets the SocketExchange object associated with the event.
     *
     * @return The SocketExchange object representing the socket connection and its associated data.
     */
    public SocketExchange getExchange() {
        return exchange;
    }

    /**
     * Gets the outgoing message data associated with the event.
     *
     * @return The outgoing message data.
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the outgoing message data for the event.
     *
     * @param data The outgoing message data to be set.
     */
    public void setData(String data) {
        this.data = data;
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
