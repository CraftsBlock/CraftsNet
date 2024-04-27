package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Cancellable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import org.jetbrains.annotations.NotNull;

/**
 * The IncomingSocketMessageEvent class represents an event related to an incoming message on a websocket connection.
 * It extends the base Event class and implements the Cancellable interface to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since CraftsNet-2.1.1
 */
public class IncomingSocketMessageEvent extends Event implements Cancellable {

    private final SocketExchange exchange;

    private final byte @NotNull [] data;
    private boolean cancelled = false;

    /**
     * Constructs a new IncomingSocketMessageEvent with the specified SocketExchange and incoming message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param data     The incoming message data for this event.
     */
    public IncomingSocketMessageEvent(SocketExchange exchange, byte @NotNull [] data) {
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
     * Gets the incoming message data associated with the event.
     *
     * @return The incoming message data.
     */
    public byte @NotNull [] getData() {
        return data;
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