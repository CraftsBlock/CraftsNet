package de.craftsblock.craftsnet.api.routing.builder;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import de.craftsblock.craftsnet.api.routing.Router;
import de.craftsblock.craftsnet.api.routing.filter.Filter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ReflectionRouteBuilder implements RouteBuilder<Exchange> {

    private final Router router;

    private final List<Filter<Exchange>> filters;

    private Handler handler;

    public ReflectionRouteBuilder(Router router) {
        this.router = router;
        this.filters = new ArrayList<>();
    }

    public ReflectionRouteBuilder setHandler(Handler handler) {
        this.handler = handler;
        return this;
    }

    public ReflectionRouteBuilder appendFilter(Filter<Exchange> filter) {
        this.filters.add(filter);
        return this;
    }

    @Override
    public @NotNull RouteInfo<Exchange> build() {
        return null;
    }

    @Override
    public @NotNull Router getRouter() {
        return router;
    }

}
