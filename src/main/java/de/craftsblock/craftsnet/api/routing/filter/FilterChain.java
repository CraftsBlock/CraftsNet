package de.craftsblock.craftsnet.api.routing.filter;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface FilterChain<E extends Exchange> {

    Object nextFilter(E exchange);

    static <T extends Exchange> FilterChain<T> newFilterChain(@NotNull List<Filter<T>> chain, @NotNull RouteInfo<T> routeInfo) {
        return new FilterChainImpl<>(chain, routeInfo);
    }

}
