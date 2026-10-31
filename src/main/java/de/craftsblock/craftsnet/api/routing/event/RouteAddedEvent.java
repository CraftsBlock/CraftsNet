package de.craftsblock.craftsnet.api.routing.event;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import de.craftsblock.craftsnet.api.routing.RouteRegistration;
import de.craftsblock.craftsnet.api.routing.Router;

public final class RouteAddedEvent extends Event {

    private final Router router;
    private final RouteRegistration registration;

    public RouteAddedEvent(Router router, RouteRegistration registration) {
        this.router = router;
        this.registration = registration;
    }

    public Router getRouter() {
        return router;
    }

    public RouteRegistration getRegistration() {
        return registration;
    }

    public RouteInfo<?> getRouteInfo() {
        return this.registration.getRouteInfo();
    }

}
