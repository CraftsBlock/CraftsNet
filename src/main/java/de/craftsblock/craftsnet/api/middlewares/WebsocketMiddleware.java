package de.craftsblock.craftsnet.api.middlewares;

import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.events.sockets.ClientConnectEvent;
import de.craftsblock.craftsnet.events.sockets.ClientDisconnectEvent;
import de.craftsblock.craftsnet.events.sockets.message.IncomingSocketMessageEvent;
import de.craftsblock.craftsnet.events.sockets.message.OutgoingSocketMessageEvent;

/**
 * A specific {@link Middleware middleware} for manipulating websocket
 * clients connections.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Middleware
 * @since 3.4.0-SNAPSHOT
 */
public interface WebsocketMiddleware extends Middleware {

    /**
     * Defines middleware logic when a new websocket client connects to the server.
     * <p>
     * <b>Note:</b> The middleware logic is applied after the {@link ClientConnectEvent}
     * and before the actual endpoint mapping logic of the websocket client.
     * </p>
     *
     * @param callbackInfo The {@link MiddlewareCallbackInfo callback info} that is used
     *                     to store data between middlewares.
     * @param exchange     The exchange holding the data of the websocket session.
     */
    default void handleConnect(MiddlewareCallbackInfo callbackInfo, SocketExchange exchange) {
        handle(callbackInfo, exchange);
    }

    /**
     * Defines middleware logic when a new websocket client disconnects from the server.
     * <p>
     * <b>Note:</b> The middleware logic is applied after the {@link ClientDisconnectEvent}
     * and before the actual disconnect logic of the websocket client. Therefore that the
     * websocket session is definitely going to be terminated the
     * {@link MiddlewareCallbackInfo#setCancelled(boolean)} statement will be ignored.
     * </p>
     *
     * @param callbackInfo The {@link MiddlewareCallbackInfo callback info} that is used
     *                     to store data between middlewares.
     * @param exchange     The exchange holding the data of the websocket session.
     */
    default void handleDisconnect(MiddlewareCallbackInfo callbackInfo, SocketExchange exchange) {
        handle(callbackInfo, exchange);
    }

    /**
     * Defines middleware logic when the server received a new message from the websocket client.
     * <p>
     * <b>Note:</b> The middleware logic is applied after the {@link IncomingSocketMessageEvent}
     * and before the actual endpoint message handling logic of the websocket client.
     * </p>
     *
     * @param callbackInfo The {@link MiddlewareCallbackInfo callback info} that is used
     *                     to store data between middlewares.
     * @param exchange     The exchange holding the data of the websocket session.
     */
    default void handleMessageReceived(MiddlewareCallbackInfo callbackInfo, SocketExchange exchange, Frame frame) {
        handle(callbackInfo, exchange);
    }

    /**
     * Defines middleware logic when the server sends a new message to the websocket client.
     * <p>
     * <b>Note:</b> The middleware logic is applied after the {@link OutgoingSocketMessageEvent}
     * and before the actual message sending of the websocket client.
     * </p>
     *
     * @param callbackInfo The {@link MiddlewareCallbackInfo callback info} that is used
     *                     to store data between middlewares.
     * @param exchange     The exchange holding the data of the websocket session.
     */
    default void handleMessageSent(MiddlewareCallbackInfo callbackInfo, SocketExchange exchange, Frame frame) {
        handle(callbackInfo, exchange);
    }

    /**
     * {@inheritDoc}
     *
     * @param callbackInfo {@inheritDoc}
     * @param exchange     {@inheritDoc}
     */
    @Override
    default void handle(MiddlewareCallbackInfo callbackInfo, BaseExchange exchange) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation checks if the server is a {@link WebSocketServer websocket server}.
     *
     * @param server {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    default boolean isApplicable(Class<? extends Server> server) {
        return WebSocketServer.class.isAssignableFrom(server);
    }

}
