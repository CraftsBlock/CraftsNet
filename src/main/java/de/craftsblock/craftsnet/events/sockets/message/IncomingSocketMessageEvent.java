package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import org.jetbrains.annotations.NotNull;

/**
 * The IncomingSocketMessageEvent class represents an event related to an incoming message on a websocket connection.
 * It extends the base {@link CancellableEvent} to support event cancellation.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @since 2.1.1-SNAPSHOT
 */
public class IncomingSocketMessageEvent extends CancellableEvent {

    private final SocketExchange exchange;

    private final byte @NotNull [] data;

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

}