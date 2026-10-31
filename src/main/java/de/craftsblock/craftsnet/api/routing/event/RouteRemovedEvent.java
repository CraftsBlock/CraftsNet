package de.craftsblock.craftsnet.api.routing.event;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import de.craftsblock.craftsnet.api.routing.Router;

public final class RouteRemovedEvent extends Event {

    private final Router router;
    private final RouteInfo<?> routeInfo;

    public RouteRemovedEvent(Router router, RouteInfo<?> routeInfo) {
        this.router = router;
        this.routeInfo = routeInfo;
    }

    public Router getRouter() {
        return router;
    }

    public RouteInfo<?> getRouteInfo() {
        return routeInfo;
    }

}
