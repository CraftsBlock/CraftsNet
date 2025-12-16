package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import org.jetbrains.annotations.NotNull;

/**
 * The OutgoingSocketMessageEvent class represents an event related to an outgoing message on a websocket connection.
 * It extends the base {@link CancellableEvent} to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see GenericSocketMessageEventBase
 * @since 2.1.1-SNAPSHOT
 */
public class OutgoingSocketMessageEvent extends CancellableEvent implements GenericSocketMessageEventBase {

    private final SocketExchange exchange;
    private Frame frame;

    /**
     * Constructs a new OutgoingSocketMessageEvent with the specified SocketExchange and message data.
     *
     * @param exchange The SocketExchange object representing the socket connection and its associated data.
     * @param frame    The {@link Frame} that contains the message information.
     */
    public OutgoingSocketMessageEvent(@NotNull SocketExchange exchange, @NotNull Frame frame) {
        this.exchange = exchange;
        this.frame = frame;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull SocketExchange getExchange() {
        return exchange;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull Frame getFrame() {
        return frame;
    }

    /**
     * Sets the opcode used to send the data.
     *
     * @param opcode The opcode which should be used.
     */
    public void setOpcode(@NotNull Opcode opcode) {
        this.frame.setOpcode(opcode);
    }

    /**
     * Sets the outgoing message data for the event.
     *
     * @param data The outgoing message data to be set.
     */
    public void setData(byte @NotNull [] data) {
        this.frame.setData(data);
    }

    /**
     * Sets the outgoing message frame.
     *
     * @param frame The outgoing message frame.
     */
    public void setFrame(@NotNull Frame frame) {
        this.frame = frame;
    }

}
