package de.craftsblock.craftsnet.api.routing.builder;

import de.craftsblock.craftsnet.api.EndpointHandler;
import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.routing.RouteSearchResult;
import de.craftsblock.craftsnet.api.routing.ServerType;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import org.apache.commons.io.output.RandomAccessFileOutputStream;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

class ReflectionRouteHandler implements Function<Exchange, Object> {

    private final ServerType serverType;
    private final EndpointHandler handler;
    private final Method method;
    private final String path;

    public ReflectionRouteHandler(ServerType serverType, EndpointHandler handler, Method method, String path) {
        this.serverType = serverType;
        this.handler = handler;
        this.method = method;
        this.path = path;
    }

    @Override
    public Object apply(Exchange exchange) {
        this.serverType.canHandleOrThrow(exchange);

        if (exchange instanceof HttpExchange httpExchange) {
            return this.applyHttp(httpExchange);
        } else if (exchange instanceof WebSocketExchange webSocketExchange) {
            return this.applyWebSocket(webSocketExchange);
        } else {
            throw new IllegalStateException("Unknown exchange " + exchange.getClass().getSimpleName() + " can't be handled");
        }
    }

    private Object applyHttp(HttpExchange exchange) {

        return null;
    }

    private Object applyWebSocket(WebSocketExchange exchange) {

        return null;
    }

    @Override
    public String toString() {
        return handler.toString();
    }
}
