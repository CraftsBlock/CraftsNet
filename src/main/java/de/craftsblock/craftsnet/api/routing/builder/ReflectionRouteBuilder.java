package de.craftsblock.craftsnet.api.routing.builder;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.EndpointHandler;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import de.craftsblock.craftsnet.api.routing.Router;
import de.craftsblock.craftsnet.api.routing.ServerType;
import de.craftsblock.craftsnet.api.routing.filter.Filter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class ReflectionRouteBuilder implements RouteBuilder<Exchange> {

    private final Router router;

    private final List<Filter<Exchange>> filters;

    private EndpointHandler handler;

    public ReflectionRouteBuilder(Router router) {
        this.router = router;
        this.filters = new ArrayList<>();
    }

    public ReflectionRouteBuilder setHandler(EndpointHandler handler) {
        this.handler = handler;
        return this;
    }

    public ReflectionRouteBuilder appendGlobalFilter(Filter<Exchange> filter) {
        this.filters.add(filter);
        return this;
    }

    @Override
    public @NotNull List<@NotNull RouteInfo<Exchange>> build() {
        var handlerType = handler.getClass();

        var basePaths = collectBasePaths();
        var baseFilters = this.collectFilters(handlerType, this.filters);

        var methods = Arrays.stream(handlerType.getDeclaredMethods())
                .filter(method -> !method.isBridge() && !method.isSynthetic())
                .toList();

        List<RouteInfo<Exchange>> routeInfos = new ArrayList<>();
        for (Method method : methods) {
            ServerType serverType = determineServerType(method);
            if (serverType == null) {
                continue;
            }

            String path = getEndpointPath(serverType, method, basePaths);
            List<Filter<Exchange>> filters = collectFilters(method, baseFilters);

            routeInfos.add(new RouteInfo<>(
                    serverType,
                    path,
                    buildHandler(),
                    filters,
                    !path.contains("{") && !path.contains("}")
            ));
        }

        return routeInfos;
    }

    private EnumMap<ServerType, String> collectBasePaths() {
        EnumMap<ServerType, String> basePaths = new EnumMap<>(ServerType.class);

        for (ServerType serverType : ServerType.values()) {
            basePaths.put(serverType, serverType.extractPath(this.handler.getClass()));
        }

        return basePaths;
    }

    private List<Filter<Exchange>> collectFilters(AnnotatedElement element, List<Filter<Exchange>> base) {
        List<Filter<Exchange>> filters = new ArrayList<>(base);

        // TODO: Implement

        return filters;
    }

    private @NotNull Function<Exchange, Object> buildHandler() {
        return exchange -> null;
    }

    private ServerType determineServerType(Method method) {
        ServerType serverType = null;

        for (ServerType current : ServerType.values()) {
            if (!method.isAnnotationPresent(current.getPathAnnotation())) {
                continue;
            }

            if (serverType != null) {
                throw new IllegalStateException("A endpoint handler can not handle multiple server types");
            }

            serverType = current;
        }

        return serverType;
    }

    private String getEndpointPath(ServerType serverType, Method method, EnumMap<ServerType, String> basePaths) {
        String base = basePaths.get(serverType);
        String path = serverType.extractPath(method);

        return base == null ? path : base + path;
    }

    @Override
    public @NotNull Router getRouter() {
        return router;
    }

}
