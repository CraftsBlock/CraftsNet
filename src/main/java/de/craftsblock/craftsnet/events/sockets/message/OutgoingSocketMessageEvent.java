package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.event.Cancellable;
import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * The OutgoingSocketMessageEvent class represents an event related to an outgoing message on a websocket connection.
 * It extends the base {@link CancellableEvent} to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since 2.1.1-SNAPSHOT
 */
public class OutgoingSocketMessageEvent extends CancellableEvent {

    private final SocketExchange exchange;

    private Opcode opcode;
    private byte @NotNull [] data;

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param opcode   The opcode that will be used to identify the message content.
     * @param data     The outgoing message data as it's string representation
     */
    public OutgoingSocketMessageEvent(@NotNull SocketExchange exchange, Opcode opcode, @NotNull String data) {
        this(exchange, opcode, data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param opcode   The opcode that will be used to identify the message content.
     * @param data     The outgoing message data as a byte array
     */
    public OutgoingSocketMessageEvent(@NotNull SocketExchange exchange, Opcode opcode, byte @NotNull [] data) {
        this.exchange = exchange;
        this.opcode = opcode;
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
     * Gets the opcode used when sending the data.
     *
     * @return The opcode of the message.
     */
    public Opcode getOpcode() {
        return opcode;
    }

    /**
     * Sets the opcode used to send the data.
     *
     * @param opcode The opcode which should be used.
     */
    public void setOpcode(Opcode opcode) {
        this.opcode = opcode;
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

}
