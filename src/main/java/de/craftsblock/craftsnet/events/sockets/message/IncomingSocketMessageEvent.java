package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.events.sockets.GenericSocketEventBase;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * The IncomingSocketMessageEvent class represents an event related to an incoming message on a websocket connection.
 * It extends the base {@link CancellableEvent} to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.0
 * @see GenericSocketMessageEventBase
 * @since 2.1.1-SNAPSHOT
 */
public class IncomingSocketMessageEvent extends CancellableEvent implements GenericSocketMessageEventBase {

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
    public Frame getFrame() {
        return frame;
    }

}