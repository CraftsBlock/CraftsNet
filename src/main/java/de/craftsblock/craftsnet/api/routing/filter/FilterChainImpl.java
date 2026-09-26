package de.craftsblock.craftsnet.api.routing.filter;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

final class FilterChainImpl<E extends Exchange> implements FilterChain<E> {

    private final @NotNull HashSet<Filter<E>> filters;
    private final @NotNull ListIterator<Filter<E>> chain;
    private final @NotNull RouteInfo<E> routeInfo;

    public FilterChainImpl(@NotNull List<Filter<E>> filters, @NotNull RouteInfo<E> routeInfo) {
        this.filters = new HashSet<>(filters);
        this.chain = filters.listIterator();
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

    @Override
    public boolean overlaps(FilterChain<?> raw) {
        if (!(raw instanceof FilterChainImpl<?> that)) {
            return true;
        }

        return this.filters.stream()
                .filter(Filter::canDistinguish)
                .anyMatch(filter -> that.filters.stream()
                        .filter(Filter::canDistinguish)
                        .anyMatch(filter::overlaps)
                );
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        FilterChainImpl<?> that = (FilterChainImpl<?>) object;
        return Objects.equals(this.filters, that.filters)
                && Objects.equals(this.routeInfo, that.routeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filters);
    }

}
