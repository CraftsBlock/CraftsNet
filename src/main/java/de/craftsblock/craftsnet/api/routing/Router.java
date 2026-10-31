package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.EndpointHandler;
import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;
import de.craftsblock.craftsnet.api.routing.builder.LambdaRouteBuilder;
import de.craftsblock.craftsnet.api.routing.builder.ReflectionRouteBuilder;
import de.craftsblock.craftsnet.api.routing.filter.Filter;
import de.craftsblock.craftsnet.api.routing.filter.http.HttpMethodFilter;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.UnmodifiableView;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public sealed abstract class Router permits RouterImpl {

    public abstract void configure(Consumer<RouterConfiguration> configurationHandler);

    public abstract <E extends Exchange> void registerFilterDefault(Class<E> exchange, Filter<E> defaultValue);

    public List<RouteSearchResult<?>> lookup(@NotNull Exchange exchange) {
        Scheme scheme = exchange.scheme();
        ServerType serverType = this.getServerType(scheme);

        return this.lookup(serverType, serverType.extractPath(exchange));
    }

    public List<RouteSearchResult<?>> lookup(@NotNull Scheme scheme, @NotNull String path) {
        return this.lookup(this.getServerType(scheme), path);
    }

    abstract List<RouteSearchResult<?>> lookup(@NotNull ServerType serverType, @NotNull String path);

    public @NotNull @UnmodifiableView List<RouteRegistration> register(@NotNull EndpointHandler handler) {
        return this.register(handler, (ignored) -> {
        });
    }

    public abstract @NotNull @UnmodifiableView List<RouteRegistration> register(@NotNull EndpointHandler handler,
                                                                                @NotNull Consumer<ReflectionRouteBuilder> routeBuilderConsumer);

    public @NotNull RouteRegistration http(
            @NotNull String path,
            @NotNull Consumer<LambdaRouteBuilder<HttpExchange, Request, Response>> routeBuilderConsumer) {

        return this.http(
                (builder) -> {
                    builder.path(path);
                    routeBuilderConsumer.accept(builder);
                }
        );
    }

    public @NotNull RouteRegistration http(
            @NotNull Consumer<LambdaRouteBuilder<HttpExchange, Request, Response>> routeBuilderConsumer) {

        return this.registerLambda(LambdaRouteBuilder.http(this), routeBuilderConsumer);
    }

    public @NotNull RouteRegistration webSocket(
            @NotNull String path,
            @NotNull Consumer<LambdaRouteBuilder<WebSocketExchange, WebSocketClient, WebSocketServer>> routeBuilderConsumer) {

        return this.webSocket(
                (builder) -> {
                    builder.path(path);
                    routeBuilderConsumer.accept(builder);
                }
        );
    }

    public @NotNull RouteRegistration webSocket(
            @NotNull Consumer<LambdaRouteBuilder<WebSocketExchange, WebSocketClient, WebSocketServer>> routeBuilderConsumer) {

        return this.registerLambda(LambdaRouteBuilder.webSocket(this), routeBuilderConsumer);
    }

    abstract <E extends Exchange, A, B> @NotNull RouteRegistration registerLambda(
            @NotNull LambdaRouteBuilder<E, A, B> builder,
            @NotNull Consumer<LambdaRouteBuilder<E, A, B>> routeBuilderConsumer);

    public RouteInfo<?> findFirstConflict(RouteInfo<?> routeInfo) {
        List<RouteSearchResult<?>> results = this.lookup(routeInfo.serverType(), routeInfo.path());

        for (RouteSearchResult<?> result : results) {
            if (result.isParameterized() == routeInfo.parameterized() &&
                    result.routeInfo().filterChain().overlaps(routeInfo.filterChain())) {
                return result.routeInfo();
            }
        }

        return null;
    }

    abstract @NotNull RouteRegistration addRoute(@NotNull RouteInfo<?> routeInfo);

    abstract void unregister(@NotNull RouteInfo<?> routeInfo);

    public @NotNull ServerType getServerType(@NotNull Scheme scheme) {
        return switch (scheme) {
            case HTTP, HTTPS -> ServerType.HTTP;
            case WS, WSS -> ServerType.WS;
        };
    }

    public static Router newRouter(CraftsNet craftsNet) {
        return new RouterImpl(craftsNet);
    }

}
