package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

public class ClientConnectEvent extends Event implements Cancelable {

    private final SocketExchange exchange;
    private final RouteRegistry.SocketMapping mapping;
    private boolean cancelled = false;
    private String reason;

    public ClientConnectEvent(SocketExchange exchange, RouteRegistry.SocketMapping mapping) {
        this.exchange = exchange;
        this.mapping = mapping;
    }

    public SocketExchange getExchange() {
        return exchange;
    }

    public RouteRegistry.SocketMapping getMapping() {
        return mapping;
    }

    public boolean hasMapping() {
        return mapping != null;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

}
