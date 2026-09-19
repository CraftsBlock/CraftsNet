package de.craftsblock.craftsnet.api.routing.filter;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;

final class FilterChainImpl<E extends Exchange> implements FilterChain<E> {

    private final @NotNull ListIterator<Filter<E>> chain;
    private final @NotNull RouteInfo<E> routeInfo;

    public FilterChainImpl(@NotNull List<Filter<E>> chain, @NotNull RouteInfo<E> routeInfo) {
        this.chain = chain.listIterator();
        this.routeInfo = routeInfo;
    }

    @Override
    public @Nullable Object nextFilter(E exchange) {
        if (!chain.hasNext()) {
            return routeInfo.handle(exchange);
        }

        Filter<E> filter = this.chain.next();
        return filter.filter(exchange, this);
    }

}
