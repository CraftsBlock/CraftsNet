package de.craftsblock.craftsnet.api.routing;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class RouteRegistration implements AutoCloseable {

    private final @NotNull Router router;
    private final @NotNull RouteInfo<?> routeInfo;

    private final AtomicBoolean registered = new AtomicBoolean(true);

    RouteRegistration(@NotNull Router router, @NotNull RouteInfo<?> routeInfo) {
        this.router = router;
        this.routeInfo = routeInfo;
    }

    public void unregister() {
        if (registered.compareAndSet(true, false)) {
            router.unregister(routeInfo);
        }
    }

    boolean unregister(RouteInfo<?> routeInfo) {
        if (this.routeInfo.equals(routeInfo)) {
            this.registered.set(false);
            return true;
        }

        return false;
    }

    public @NotNull Router getRouter() {
        return router;
    }

    public @NotNull RouteInfo<?> getRouteInfo() {
        return routeInfo;
    }

    public boolean isRegistered() {
        return registered.get();
    }

    @Override
    public void close() {
        this.unregister();
    }

}
