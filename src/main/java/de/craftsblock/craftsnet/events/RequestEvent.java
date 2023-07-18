package de.craftsblock.craftsnet.events;

import de.craftsblock.craftscore.event.Cancelable;
import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Exchange;

public class RequestEvent extends Event implements Cancelable {

    private final Exchange exchange;

    private final RouteRegistry.RouteMapping mapping;
    private boolean cancelled = false;

    public RequestEvent(Exchange exchange, RouteRegistry.RouteMapping mapping) {
        this.exchange = exchange;
        this.mapping = mapping;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public RouteRegistry.RouteMapping getRoute() {
        return mapping;
    }

    public boolean hasRoute() {
        return mapping != null;
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
