package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the base for all websocket events.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see WebSocketExchange
 * @see WebSocketClient
 * @see WebSocketServer
 * @since 3.4.0-SNAPSHOT
 */
public interface GenericSocketEventBase {

    /**
     * Gets the {@link WebSocketExchange} which stores the involved client, server and some other data.
     *
     * @return The {@link WebSocketExchange}.
     */
    @NotNull
    WebSocketExchange getExchange();

    /**
     * Gets the {@link WebSocketClient} which is involved in this event.
     *
     * @return The {@link WebSocketClient}.
     * @since 3.4.0-SNAPSHOT
     */
    default @NotNull WebSocketClient getClient() {
        return getExchange().client();
    }

    /**
     * Gets the {@link WebSocketServer} which is involved in this event.
     *
     * @return The {@link WebSocketServer}.
     * @since 3.4.0-SNAPSHOT
     */
    default @NotNull WebSocketServer getServer() {
        return getExchange().server();
    }

}
