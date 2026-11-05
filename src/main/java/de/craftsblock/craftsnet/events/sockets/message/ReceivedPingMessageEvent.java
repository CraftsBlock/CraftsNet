package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.events.sockets.GenericSocketEventBase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An event indicating that a Ping message has been received.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see GenericSocketMessageEventBase
 * @since 3.0.5-SNAPSHOT
 */
public class ReceivedPingMessageEvent extends Event implements GenericSocketMessageEventBase {

    private final SocketExchange exchange;
    private final Frame frame;

    /**
     * Constructs a ReceivedPingMessageEvent with the specified SocketExchange.
     *
     * @param exchange The SocketExchange associated with the received Pong message.
     * @param frame    The {@link Frame frame} send with the ping message.
     */
    public ReceivedPingMessageEvent(SocketExchange exchange, @NotNull Frame frame) {
        this.exchange = exchange;
        this.frame = frame;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean isAsyncAllowed() {
        return false;
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
