package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.api.routing.filter.Filter;
import de.craftsblock.craftsnet.api.routing.filter.FilterChain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public record RouteInfo<E extends Exchange>(
        @NotNull ServerType serverType,
        @NotNull String path,
        @NotNull Function<E, Object> handler,
        @NotNull @UnmodifiableView List<Filter<E>> filters,
        boolean direct) {

    public RouteInfo(
            @NotNull ServerType serverType,
            @NotNull String path,
            @NotNull Function<E, Object> handler,
            @NotNull List<Filter<E>> filters,
            boolean direct) {
        this.serverType = serverType;
        this.path = path;
        this.handler = handler;
        this.filters = List.copyOf(filters);
        this.direct = direct;
    }

    public Object handle(E exchange) {
        return filterChain().nextFilter(exchange);
    }

    public FilterChain<E> filterChain() {
        return FilterChain.newFilterChain(this.filters, this);
    }

}
