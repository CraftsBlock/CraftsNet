package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.api.websocket.annotations.MessageReceiver;
import de.craftsblock.craftsnet.api.websocket.annotations.Socket;

/**
 * The SocketExchange record represents an exchange object that provides a way to interact
 * with the WebSocket server and client within the context of a WebSocket connection.
 * It allows sending broadcast messages to all clients connected to the same WebSocket path.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see Socket
 * @see SocketHandler
 * @see MessageReceiver
 * @since 2.1.1
 */
public record SocketExchange(WebSocketServer server, WebSocketClient client) {

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

}
