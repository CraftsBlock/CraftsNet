package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Cancellable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.ControlByte;
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
    private ControlByte controlByte;
    private byte @NotNull [] data;

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange    The SocketExchange object representing the socket connection and its associated data.
     * @param controlByte The control byte that will be used to identify the message content.
     * @param data        The outgoing message data as it's string representation
     */
    public OutgoingSocketMessageEvent(@NotNull SocketExchange exchange, ControlByte controlByte, @NotNull String data) {
        this(exchange, controlByte, data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange    The SocketExchange object representing the socket connection and its associated data.
     * @param controlByte The control byte that will be used to identify the message content.
     * @param data        The outgoing message data as a byte array
     */
    public OutgoingSocketMessageEvent(@NotNull SocketExchange exchange, ControlByte controlByte, byte @NotNull [] data) {
        this.exchange = exchange;
        this.controlByte = controlByte;
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
     * Gets the control byte used when sending the data.
     *
     * @return The control byte of the message.
     */
    public ControlByte getControlByte() {
        return controlByte;
    }

    /**
     * Sets the control byte used to send the data.
     *
     * @param controlByte The control byte which should be used.
     */
    public void setControlByte(ControlByte controlByte) {
        this.controlByte = controlByte;
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
