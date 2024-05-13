package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import org.jetbrains.annotations.Nullable;

/**
 * An event indicating that a Pong message has been received.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.5-SNAPSHOT
 */
public class ReceivedPongMessageEvent extends Event {

    private final SocketExchange exchange;
    private final byte @Nullable [] message;

    /**
     * Constructs a ReceivedPongMessageEvent with the specified SocketExchange.
     *
     * @param exchange The SocketExchange associated with the received Pong message.
     * @param message  The message as a byte array send with the pong message.
     */
    public ReceivedPongMessageEvent(SocketExchange exchange, byte @Nullable [] message) {
        this.exchange = exchange;
        this.message = message;
    }

    /**
     * Retrieves the message as a byte array that was sent with the ping message.
     *
     * @return The message as a byte array if present, otherwise null.
     */
    public byte @Nullable [] getMessage() {
        return message;
    }

    /**
     * Retrieves the WebSocketClient associated with the Pong message.
     *
     * @return The WebSocketClient associated with the Pong message.
     */
    public WebSocketClient getClient() {
        return exchange.client();
    }

    /**
     * Retrieves the WebSocketServer associated with the Pong message.
     *
     * @return The WebSocketServer associated with the Pong message.
     */
    public WebSocketServer getServer() {
        return exchange.server();
    }

}
