package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftscore.cache.LruCache;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

class RoutingCache {

    private final ReentrantLock lock;

    private volatile boolean enabled;
    private volatile LruCache<RouteCacheKey, List<RouteSearchResult<?>>> routeCache;
    private final ConcurrentHashMap<RouteCacheKey, Object> computationLocks = new ConcurrentHashMap<>();

    RoutingCache(boolean enabled, int cacheSize) {
        this.lock = new ReentrantLock();
        this.enabled = enabled;
        this.routeCache = new LruCache<>(cacheSize);
    }

    public List<RouteSearchResult<?>> computeIfAbsent(
            ServerType serverType,
            String path,
            Supplier<List<RouteSearchResult<?>>> resultSupplier
    ) {

        if (!enabled) {
            return resultSupplier.get();
        }

        RouteCacheKey cacheKey = new RouteCacheKey(serverType, path);
        Object computationLock = computationLocks.computeIfAbsent(
                cacheKey,
                key -> new Object()
        );

        synchronized (computationLock) {
            try {
                var cachedAfterLock = getCached(cacheKey);
                if (cachedAfterLock != null) {
                    return cachedAfterLock;
                }

                var result = resultSupplier.get();
                if (enabled) {
                    withLock(() -> routeCache.put(cacheKey, result));
                }

                return result;
            } finally {
                computationLocks.remove(cacheKey, computationLock);
            }
        }
    }

    private List<RouteSearchResult<?>> getCached(RouteCacheKey cacheKey) {
        if (!enabled) {
            return null;
        }

        AtomicReference<List<RouteSearchResult<?>>> result = new AtomicReference<>();
        this.withLock(() -> result.set(routeCache.get(cacheKey)));
        return result.get();
    }

    public void clear() {
        this.withLock(() -> this.routeCache.clear());
    }

    public void setEnabled(boolean enabled) {
        this.withLock(() -> {
            this.enabled = enabled;

            if (!enabled) {
                this.routeCache.clear();
            }
        });
    }

    public void resize(int cacheSize) {
        if (cacheSize < 1) {
            throw new IllegalArgumentException("Cache size must be >= 1");
        }

        this.withLock(() -> {
            if (routeCache.getCapacity() == cacheSize) {
                return;
            }

            LruCache<RouteCacheKey, List<RouteSearchResult<?>>> newCache =
                    new LruCache<>(cacheSize);

            newCache.putAll(this.routeCache);
            this.routeCache = newCache;
        });
    }

    private void withLock(Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    private record RouteCacheKey(
            ServerType serverType,
            String path
    ) {
    }

}
