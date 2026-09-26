package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;
import de.craftsblock.craftsnet.api.routing.builder.LambdaRouteBuilder;
import de.craftsblock.craftsnet.api.routing.builder.ReflectionRouteBuilder;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Router {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final RouterConfiguration configuration;

    private final EnumMap<ServerType, Map<String, List<RouteRegistration>>> directRoutes = new EnumMap<>(ServerType.class);
    private final EnumMap<ServerType, RoutingTrie> routingTries = new EnumMap<>(ServerType.class);

    private final RoutingCache routingCache;

    public Router() {
        this(new RouterConfiguration());
    }

    public Router(RouterConfiguration configuration) {
        this.configuration = configuration;

        this.directRoutes.put(ServerType.HTTP, new HashMap<>());
        this.directRoutes.put(ServerType.WS, new HashMap<>());

        this.routingTries.put(ServerType.HTTP, new RoutingTrie());
        this.routingTries.put(ServerType.WS, new RoutingTrie());

        this.routingCache = new RoutingCache(configuration.isCacheEnabled(), configuration.getCacheSize());
    }

    public void configure(Consumer<RouterConfiguration> configurationHandler) {
        configurationHandler.accept(this.configuration);

        this.routingCache.setEnabled(this.configuration.isCacheEnabled());
        this.routingCache.resize(this.configuration.getCacheSize());
    }

    public List<RouteSearchResult<?>> lookup(@NotNull Exchange exchange) {
        Scheme scheme = exchange.scheme();
        ServerType serverType = this.getServerType(scheme);

        return this.lookup(serverType, serverType.extractPath(exchange));
    }

    public List<RouteSearchResult<?>> lookup(@NotNull Scheme scheme, @NotNull String path) {
        return this.lookup(this.getServerType(scheme), path);
    }

    private List<RouteSearchResult<?>> lookup(@NotNull ServerType serverType, @NotNull String path) {
        var directRoutes = withLock(readLock, () -> this.directRoutes.get(serverType).get(path));
        if (directRoutes != null && !directRoutes.isEmpty()) {
            return directRoutes.stream()
                    .<RouteSearchResult<?>>map((registration) -> RouteSearchResult.of(registration.getRouteInfo()))
                    .toList();
        }

        return routingCache.computeIfAbsent(
                serverType,
                path,
                () -> withLock(readLock, () ->
                        routingTries.get(serverType).lookup(path)
                )
        );
    }

    public @NotNull @UnmodifiableView List<RouteRegistration> register(@NotNull Handler handler) {
        return this.register(handler, (ignored) -> {
        });
    }

    public @NotNull @UnmodifiableView List<RouteRegistration> register(@NotNull Handler handler,
                                                                       @NotNull Consumer<ReflectionRouteBuilder> routeBuilderConsumer) {
        ReflectionRouteBuilder builder = new ReflectionRouteBuilder(this);
        builder.setHandler(handler);

        routeBuilderConsumer.accept(builder);

        List<RouteInfo<Exchange>> routeInfos = builder.build();
        List<RouteRegistration> registrations = new ArrayList<>(routeInfos.size());
        for (RouteInfo<Exchange> routeInfo : routeInfos) {
            registrations.add(this.addRoute(routeInfo));
        }

        return Collections.unmodifiableList(registrations);
    }

    public @NotNull RouteRegistration http(
            @NotNull String path,
            @NotNull Consumer<LambdaRouteBuilder<HttpExchange, Request, Response>> routeBuilderConsumer) {

        return this.registerLambda(
                LambdaRouteBuilder.http(this),
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

    public @NotNull RouteRegistration webSocket(
            @NotNull String path,
            @NotNull Consumer<LambdaRouteBuilder<WebSocketExchange, WebSocketClient, WebSocketServer>> routeBuilderConsumer) {

        return this.registerLambda(
                LambdaRouteBuilder.webSocket(this),
                (builder) -> {
                    builder.path(path);
                    routeBuilderConsumer.accept(builder);
                }
        );
    }

    private <E extends Exchange, A, B> @NotNull RouteRegistration registerLambda(
            @NotNull LambdaRouteBuilder<E, A, B> builder,
            @NotNull Consumer<LambdaRouteBuilder<E, A, B>> routeBuilderConsumer) {

        routeBuilderConsumer.accept(builder);
        return this.addRoute(builder.build().get(0));
    }

    private @NotNull RouteRegistration addRoute(@NotNull RouteInfo<?> routeInfo) {
        ServerType serverType = routeInfo.serverType();
        RouteRegistration registration = new RouteRegistration(this, routeInfo);

        var serverDirectRoutes = this.directRoutes.get(serverType);
        var serverRoutingTrie = this.routingTries.get(serverType);

        withLock(writeLock, () -> {
            this.routingCache.clear();

            if (routeInfo.direct()) {
                serverDirectRoutes
                        .computeIfAbsent(routeInfo.path(), p -> new ArrayList<>())
                        .add(registration);
                return;
            }

            serverRoutingTrie.insert(registration);
        });

        return registration;
    }

    void unregister(@NotNull RouteInfo<?> routeInfo) {
        ServerType serverType = routeInfo.serverType();

        var serverDirectRoutes = this.directRoutes.get(serverType);
        var serverRoutingTrie = this.routingTries.get(serverType);

        withLock(writeLock, () -> {
            if (!routeInfo.direct()) {
                serverRoutingTrie.remove(routeInfo);
                return;
            }

            String path = routeInfo.path();
            var routes = serverDirectRoutes.get(routeInfo.path());
            if (routes == null) {
                return;
            }

            routes.removeIf(routeRegistration -> routeRegistration.unregister(routeInfo));
            if (routes.isEmpty()) {
                serverDirectRoutes.remove(path);
            }
        });

        this.routingCache.clear();
    }

    void withLock(@NotNull Lock lock, @NotNull Runnable runnable) {
        withLock(lock, () -> {
            runnable.run();
            return null;
        });
    }

    <T> T withLock(@NotNull Lock lock, @NotNull Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @TestOnly
    public RoutingTrie getRoutingTrie(ServerType serverType) {
        return routingTries.get(serverType);
    }

    public @NotNull ServerType getServerType(@NotNull Scheme scheme) {
        return switch (scheme) {
            case HTTP, HTTPS -> ServerType.HTTP;
            case WS, WSS -> ServerType.WS;
        };
    }

}
