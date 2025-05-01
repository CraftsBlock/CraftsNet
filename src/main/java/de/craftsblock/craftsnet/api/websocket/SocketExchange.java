package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.utils.ProtocolVersion;
import de.craftsblock.craftsnet.api.websocket.annotations.Socket;
import org.jetbrains.annotations.NotNull;

/**
 * The SocketExchange record represents an exchange object that provides a way to interact
 * with the WebSocket server and client within the context of a WebSocket connection.
 * It allows sending broadcast messages to all clients connected to the same WebSocket path.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.3.0
 * @see BaseExchange
 * @see Socket
 * @see SocketHandler
 * @since 2.1.1-SNAPSHOT
 */
public record SocketExchange(@NotNull ProtocolVersion protocolVersion,
                             @NotNull WebSocketServer server, @NotNull WebSocketClient client) implements BaseExchange {

    /**
     * @param protocolVersion The {@link ProtocolVersion} object containing the protocol version used.
     * @param server          The {@link WebSocketServer} object that the client connected to.
     * @param client          The {@link WebSocketClient} object representing the websocket connection.
     */
    public SocketExchange {
    }

    /**
     * Gets the {@link WebSocketServer} object associated with this exchange
     * the websocket client connected to.
     *
     * @return The {@link WebSocketServer} object.
     */
    @Override
    public WebSocketServer server() {
        return server;
    }

    /**
     * Gets the {@link WebSocketClient} object associated with this exchange
     * representing the websocket connection.
     *
     * @return The {@link WebSocketClient} object.
     */
    @Override
    public WebSocketClient client() {
        return client;
    }

    /**
     * Broadcasts the given data to all WebSocket clients connected to the same path as the current client.
     * If the current client does not have a WebSocket path associated, the broadcast is not performed.
     *
     * @param data The data to be broadcasted to the WebSocket clients.
     */
    public void broadcast(String data) {
        if (client.getPath() == null)
            return;
        server.broadcast(client.getPath(), data);
    }

    /**
     * Performs last actions before the exchange is closed.
     */
    @Override
    public void close() throws Exception {
    }

}
