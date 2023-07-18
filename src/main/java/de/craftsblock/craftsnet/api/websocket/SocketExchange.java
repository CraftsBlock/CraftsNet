package de.craftsblock.craftsnet.api.websocket;

public record SocketExchange(WebSocketServer server, WebSocketClient client) {

    public void broadcast(String data) {
        server.broadcast(data);
    }

}
