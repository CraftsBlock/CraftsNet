package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;

import java.util.function.Function;

public enum ServerType {

    HTTP(WebServer.class, HttpExchange.class, exchange -> exchange.request().getUrl()),
    WS(WebSocketServer.class, WebSocketExchange.class, exchange -> exchange.client().getPath()),
    ;

    private final Class<? extends Server> serverClass;
    private final Class<? extends Exchange> exchangeClass;
    private final Function<Exchange, String> pathExtractor;

    <E extends Exchange> ServerType(
            Class<? extends Server> serverClass,
            Class<E> exchangeClass,
            Function<E, String> pathExtractor
    ) {
        this.serverClass = serverClass;
        this.exchangeClass = exchangeClass;
        this.pathExtractor = exchange -> pathExtractor.apply(exchangeClass.cast(exchange));
    }

    public Class<? extends Server> getServerClass() {
        return serverClass;
    }

    public String extractPath(Exchange exchange) {
        if (!TypeUtils.isInstance(exchangeClass, exchange)) {
            throw new IllegalArgumentException(
                    "Expected " + exchangeClass.getName()
                            + " but got " + exchange.getClass().getName());
        }

        return pathExtractor.apply(exchange);
    }
}