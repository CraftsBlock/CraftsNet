package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Cancellable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * The OutgoingSocketMessageEvent class represents an event related to an outgoing message on a websocket connection.
 * It extends the base Event class and implements the Cancellable interface to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since CraftsNet-2.1.1
 */
public class OutgoingSocketMessageEvent extends Event implements Cancellable {

    private final SocketExchange exchange;
    private boolean cancelled = false;

    private byte @NotNull [] data;

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param data  The outgoing message data as it's string representation
     */
    public OutgoingSocketMessageEvent(@NotNull SocketExchange exchange, @NotNull String data) {
        this(exchange, data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param data  The outgoing message data as a byte array
     */
    public OutgoingSocketMessageEvent(@NotNull SocketExchange exchange, byte @NotNull [] data) {
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
    public byte @NotNull [] getData() {
        return data;
    }

    /**
     * Sets the outgoing message data for the event.
     *
     * @param data The outgoing message data to be set.
     */
    public void setData(byte @NotNull [] data) {
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
