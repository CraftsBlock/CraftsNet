package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An event indicating that a Pong message has been received.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see GenericSocketMessageEventBase
 * @since 3.0.5-SNAPSHOT
 */
public class ReceivedPongMessageEvent extends Event implements GenericSocketMessageEventBase {

    private final WebSocketExchange exchange;
    private final Frame frame;

    /**
     * Constructs a ReceivedPongMessageEvent with the specified WebSocketExchange.
     *
     * @param exchange The WebSocketExchange associated with the received Pong message.
     * @param frame    The {@link Frame frame} send with the pong message.
     */
    public ReceivedPongMessageEvent(WebSocketExchange exchange, @NotNull Frame frame) {
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
    public @NotNull WebSocketExchange getExchange() {
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

    /**
     * Retrieves the message as a byte array that was sent with the ping message.
     *
     * @return The message as a byte array if present, otherwise null.
     * @deprecated Use {@link #getData()} instead.
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    @Deprecated(forRemoval = true, since = "3.4.0-SNAPSHOT")
    public byte @Nullable [] getMessage() {
        return getData();
    }

}
