package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.EndpointHandler;
import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.routing.builder.LambdaRouteBuilder;
import de.craftsblock.craftsnet.api.routing.builder.ReflectionRouteBuilder;
import de.craftsblock.craftsnet.api.routing.event.RouteAddedEvent;
import de.craftsblock.craftsnet.api.routing.event.RouteRemovedEvent;
import de.craftsblock.craftsnet.api.routing.filter.Filter;
import de.craftsblock.craftsnet.api.routing.filter.http.HttpMethodFilter;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class RouterImpl extends Router {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final CraftsNet craftsNet;
    private final RouterConfiguration configuration;

    private final EnumMap<ServerType, Map<String, List<RouteRegistration>>> directRoutes = new EnumMap<>(ServerType.class);
    private final EnumMap<ServerType, RoutingTrie> routingTries = new EnumMap<>(ServerType.class);
    private final EnumMap<ServerType, Map<Class<?>, Filter<?>>> defaultFilters = new EnumMap<>(ServerType.class);

    private final RoutingCache routingCache;

    public RouterImpl(CraftsNet craftsNet) {
        this(craftsNet, new RouterConfiguration());
    }

    public RouterImpl(CraftsNet craftsNet, RouterConfiguration configuration) {
        this.craftsNet = craftsNet;
        this.configuration = configuration;

        this.directRoutes.put(ServerType.HTTP, new HashMap<>());
        this.directRoutes.put(ServerType.WS, new HashMap<>());

        this.routingTries.put(ServerType.HTTP, new RoutingTrie());
        this.routingTries.put(ServerType.WS, new RoutingTrie());

        this.routingCache = new RoutingCache(configuration.isCacheEnabled(), configuration.getCacheSize());

        this.registerFilterDefault(HttpExchange.class, HttpMethodFilter.get());
    }

    @Override
    public void configure(Consumer<RouterConfiguration> configurationHandler) {
        configurationHandler.accept(this.configuration);

        this.routingCache.setEnabled(this.configuration.isCacheEnabled());
        this.routingCache.resize(this.configuration.getCacheSize());
    }

    @Override
    public <E extends Exchange> void registerFilterDefault(Class<E> exchange, Filter<E> defaultValue) {
        ServerType serverType = this.getServerType(exchange);
        defaultFilters.computeIfAbsent(serverType, s -> new HashMap<>(1))
                .put(defaultValue.getClass(), defaultValue);
    }

    @Override
    List<RouteSearchResult<?>> lookup(@NotNull ServerType serverType, @NotNull String path) {
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

    @Override
    public @NotNull @UnmodifiableView List<RouteRegistration> register(@NotNull EndpointHandler handler,
                                                                       @NotNull Consumer<ReflectionRouteBuilder> routeBuilderConsumer) {
        ReflectionRouteBuilder builder = new ReflectionRouteBuilder(this, (serverType, filters) -> {
            Map<Class<?>, Filter<?>> filterDefaults = this.defaultFilters.get(serverType);
            if (filterDefaults.isEmpty()) {
                return;
            }

            filterDefaults.forEach((type, filter) -> {
                boolean alreadyRegistered = filters.stream()
                        .anyMatch(existing -> existing.getClass().equals(type));

                if (!alreadyRegistered) {
                    filters.add(filter);
                }
            });
        });

        builder.setHandler(handler);
        routeBuilderConsumer.accept(builder);

        List<RouteInfo<Exchange>> routeInfos = builder.build();
        List<RouteRegistration> registrations = new ArrayList<>(routeInfos.size());
        for (RouteInfo<Exchange> routeInfo : routeInfos) {
            registrations.add(this.addRoute(routeInfo));
        }

        return Collections.unmodifiableList(registrations);
    }

    @Override
    @SuppressWarnings("unchecked")
    <E extends Exchange, A, B> @NotNull RouteRegistration registerLambda(
            @NotNull LambdaRouteBuilder<E, A, B> builder,
            @NotNull Consumer<LambdaRouteBuilder<E, A, B>> routeBuilderConsumer) {

        routeBuilderConsumer.accept(builder);

        Map<Class<?>, Filter<?>> filterDefaults = this.defaultFilters.get(builder.getServerType());
        if (!filterDefaults.isEmpty()) {
            filterDefaults.forEach((type, filter) -> {
                boolean alreadyRegistered = builder.getFilters().stream()
                        .anyMatch(existing -> existing.getClass().equals(type));

                if (!alreadyRegistered) {
                    builder.appendFilter((Filter<E>) filter);
                }
            });
        }

        return this.addRoute(builder.build().get(0));
    }

    @Override
    @NotNull RouteRegistration addRoute(@NotNull RouteInfo<?> routeInfo) {
        RouteInfo<?> conflict = findFirstConflict(routeInfo);
        if (conflict != null) {
            throw new IllegalStateException(
                    "Cannot register route: an overlapping handler is already registered.\n"
                            + "New route:\n"
                            + "  " + routeInfo + "\n"
                            + "Existing route:\n"
                            + "  " + conflict
            );
        }

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

        if (craftsNet != null) {
            craftsNet.getListenerRegistry().call(new RouteAddedEvent(this, registration));
        }

        return registration;
    }

    @Override
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

        if (craftsNet != null) {
            craftsNet.getListenerRegistry().call(new RouteRemovedEvent(this, routeInfo));
        }

        this.routingCache.clear();
    }

    private void withLock(@NotNull Lock lock, @NotNull Runnable runnable) {
        withLock(lock, () -> {
            runnable.run();
            return null;
        });
    }

    private <T> T withLock(@NotNull Lock lock, @NotNull Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    private @NotNull ServerType getServerType(@NotNull Class<? extends Exchange> exchange) {
        if (exchange.equals(HttpExchange.class)) {
            return ServerType.HTTP;
        }

        if (exchange.equals(WebSocketExchange.class)) {
            return ServerType.WS;
        }

        throw new IllegalStateException("Unexpected value: " + exchange.getName());
    }

}
