package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;

public class OutgoingSocketMessageEvent extends Event implements Cancelable {

    private final SocketExchange exchange;
    private boolean cancelled = false;

    private String data;

    public OutgoingSocketMessageEvent(SocketExchange exchange, String data) {
        this.exchange = exchange;
        this.data = data;
    }

    public SocketExchange getExchange() {
        return exchange;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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
