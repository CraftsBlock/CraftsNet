package de.craftsblock.craftsnet.api.routing;

import java.util.concurrent.atomic.AtomicBoolean;

public class RouteRegistration implements AutoCloseable {

    private final Router router;
    private final RouteInfo<?> routeInfo;

    private final AtomicBoolean registered = new AtomicBoolean(true);

    RouteRegistration(Router router, RouteInfo<?> routeInfo) {
        this.router = router;
        this.routeInfo = routeInfo;
    }

    public void unregister() {
        if (registered.compareAndSet(true, false)) {
            router.unregister(routeInfo);
        }
    }

    @Override
    public void close() {
        this.unregister();
    }

    public Router getRouter() {
        return router;
    }

}
