package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * The IncomingSocketMessageEvent class represents an event related to an incoming message on a websocket connection.
 * It extends the base {@link CancellableEvent} to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @since 2.1.1-SNAPSHOT
 */
public class IncomingSocketMessageEvent extends CancellableEvent {

    private final SocketExchange exchange;

    private final Frame frame;

    /**
     * Constructs a new IncomingSocketMessageEvent with the specified SocketExchange and incoming message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param frame    The incoming message frame for this event.
     */
    public IncomingSocketMessageEvent(SocketExchange exchange, @NotNull Frame frame) {
        this.exchange = exchange;
        this.frame = frame;
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
     * Gets the incoming message as a {@link Frame} object.
     *
     * @return The incoming message.
     */
    public Frame getFrame() {
        return frame;
    }

    /**
     * Gets the incoming message as a {@link ByteBuffer} object.
     *
     * @return The incoming message.
     */
    public ByteBuffer getBuffer() {
        return frame.getBuffer();
    }

    /**
     * Gets the incoming message as a byte array.
     *
     * @return The incoming message data.
     */
    public byte @NotNull [] getData() {
        return frame.getData();
    }


    /**
     * Gets the incoming message as an utf8 encoded string.
     *
     * @return The incoming message.
     */
    public String getUtf8() {
        if (!frame.getOpcode().equals(Opcode.TEXT)) return null;
        return frame.getUtf8();
    }

}