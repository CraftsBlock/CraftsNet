package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

public class ClientDisconnectEvent extends Event {

    private final SocketExchange exchange;
    private final RouteRegistry.SocketMapping mapping;

    public ClientDisconnectEvent(SocketExchange exchange, RouteRegistry.SocketMapping mapping) {
        this.exchange = exchange;
        this.mapping = mapping;
    }

    public RouteRegistry.SocketMapping getMapping() {
        return mapping;
    }

    public boolean hasMapping() {
        return mapping != null;
    }

    public SocketExchange getExchange() {
        return exchange;
    }

}
