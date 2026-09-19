package de.craftsblock.craftsnet.api.routing.builder;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import de.craftsblock.craftsnet.api.routing.Router;
import de.craftsblock.craftsnet.api.routing.ServerType;
import de.craftsblock.craftsnet.api.routing.filter.Filter;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class LambdaRouteBuilder<E extends Exchange, A, B> implements RouteBuilder<E> {

    private final Router router;
    private final ServerType serverType;

    private final List<Filter<E>> filters;

    private final Function<E, A> primary;
    private final Function<E, B> secondary;

    private String path;
    private Function<E, Object> handler;

    LambdaRouteBuilder(Router router, ServerType serverType, Function<E, A> primary, Function<E, B> secondary) {
        this.router = router;
        this.serverType = serverType;
        this.filters = new ArrayList<>();
        this.primary = primary;
        this.secondary = secondary;
    }

    public LambdaRouteBuilder<E, A, B> handle(BiConsumer<A, B> handler) {
        return this.handle((exchange) -> {
            handler.accept(this.primary.apply(exchange), this.secondary.apply(exchange));
            return null;
        });
    }

    public LambdaRouteBuilder<E, A, B> handle(BiFunction<A, B, Object> handler) {
        return this.handle((exchange) -> {
            return handler.apply(this.primary.apply(exchange), this.secondary.apply(exchange));
        });
    }

    public LambdaRouteBuilder<E, A, B> handle(Consumer<E> handler) {
        return this.handle((exchange -> {
            handler.accept(exchange);
            return null;
        }));
    }

    public LambdaRouteBuilder<E, A, B> handle(Function<E, Object> handler) {
        this.handler = handler;
        return this;
    }

    public LambdaRouteBuilder<E, A, B> path(@NotNull String path) {
        this.path = path;
        return this;
    }

    public LambdaRouteBuilder<E, A, B> appendFilter(Filter<E> filter) {
        this.filters.add(filter);
        return this;
    }

    @Override
    public @NotNull RouteInfo<E> build() {
        if (path == null) {
            throw new IllegalStateException("Route path is missing");
        }

        if (handler == null) {
            throw new IllegalStateException("Route handler is missing");
        }

        return new RouteInfo<>(
                this.serverType,
                this.path,
                this.handler,
                this.filters,
                !this.path.contains("{") && !this.path.contains("}")
        );
    }

    @Override
    public @NotNull Router getRouter() {
        return router;
    }

    public static LambdaRouteBuilder<HttpExchange, Request, Response> http(Router router) {
        return new LambdaRouteBuilder<>(router, ServerType.HTTP, HttpExchange::request, HttpExchange::response);
    }

    public static LambdaRouteBuilder<WebSocketExchange, WebSocketClient, WebSocketServer> webSocket(Router router) {
        return new LambdaRouteBuilder<>(router, ServerType.WS, WebSocketExchange::client, WebSocketExchange::server);
    }

}
