package de.craftsblock.craftsnet.api.middlewares;

import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.middlewares.annotation.ApplyMiddleware;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A registry for resolving and storing {@link Middleware middlewares}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see Middleware
 * @since 3.4.0-SNAPSHOT
 */
public class MiddlewareRegistry {

    private final Map<Class<? extends Server>, Deque<Middleware>> middlewares = new ConcurrentHashMap<>();

    public MiddlewareRegistry() {
        Server.SERVER_TYPES.forEach(type -> middlewares.put(type, new ConcurrentLinkedDeque<>()));
    }

    /**
     * Registers a {@link Middleware middleware} that should be applied
     * globally.
     *
     * @param middleware The {@link Middleware middleware} to register.
     */
    public void register(Middleware middleware) {
        if (isRegistered(middleware)) return;
        Server.SERVER_TYPES.forEach(type -> {
            if (!middleware.isApplicable(type)) return;
            middlewares.get(type).add(middleware);
        });
    }

    /**
     * Unregisters a {@link Middleware middleware} that should no longer
     * be applied globally.
     *
     * @param middleware The {@link Middleware middleware} to register.
     */
    public void unregister(Middleware middleware) {
        Server.SERVER_TYPES.forEach(type -> {
            if (!middleware.isApplicable(type)) return;
            middlewares.get(type).remove(middleware);
        });
    }

    /**
     * Checks if a specific {@link Middleware middleware} is registered
     * for global appliance.
     *
     * @param middleware The {@link Middleware middleware} to check.
     * @return {@code true} if it is register for global appliance, {@code false} otherwise.
     */
    public boolean isRegistered(Middleware middleware) {
        AtomicBoolean registered = new AtomicBoolean(true);

        Server.SERVER_TYPES.forEach(type -> {
            if (!registered.get() || !middleware.isApplicable(type)) return;
            registered.set(middlewares.get(type).contains(middleware));
        });

        return registered.get();
    }

    /**
     * Get all global applied {@link Middleware middlewares}.
     *
     * @return A {@link Deque deque} of {@link Middleware middlewares}for global appliance.
     */
    public Map<Class<? extends Server>, Deque<Middleware>> getMiddlewares() {
        return middlewares;
    }

    /**
     * Get all, for the exchange applicable, global applied
     * {@link Middleware middlewares}.
     *
     * @param exchange The exchange for which the {@link Middleware middlewares}
     *                 should be applicable.
     * @return The {@link Deque deque} of {@link Middleware middlewares} for global appliance.
     */
    public Deque<Middleware> getMiddlewares(BaseExchange exchange) {
        return getMiddlewares(exchange.scheme().getServerRaw());
    }

    /**
     * Get all, for the {@link Server server} type applicable, global
     * applied {@link Middleware middlewares}.
     *
     * @param server The {@link Server server} type for which the
     *               {@link Middleware middlewares} should be applicable.
     * @return The {@link Deque deque} of {@link Middleware middlewares} for global appliance.
     */
    public Deque<Middleware> getMiddlewares(Class<? extends Server> server) {
        return middlewares.get(server);
    }

    /**
     * Retrieves a {@link Deque stack} of {@link Middleware middlewares}
     * from a specific {@link Handler endpoint handler} nd its child
     * {@link Method method}.
     *
     * @param root    The {@link Handler endpoint handler}.
     * @param handler The {@link Method method}.
     * @return A {@link Deque stack} of {@link Middleware middlewares} that are present.
     */
    public Deque<Middleware> resolveMiddlewares(Handler root, Method handler) {
        Deque<Middleware> middelwareDeque = new ConcurrentLinkedDeque<>();

        Class<? extends Handler> type = root.getClass();
        this.resolveMiddlewares(type, middelwareDeque);
        this.resolveMiddlewares(handler, middelwareDeque);

        return middelwareDeque;
    }

    /**
     * Retrieves a {@link Deque deque} of {@link Middleware middlewares}
     * from a {@link AnnotatedElement} and push it into an existing deque.
     *
     * @param element The {@link AnnotatedElement} to search on.
     * @param deque   The {@link Stack deque} where the result should be pushed to.
     */
    private void resolveMiddlewares(AnnotatedElement element, Deque<Middleware> deque) {
        this.unpackMiddleware(ReflectionUtils.retrieveRawAnnotation(element, ApplyMiddleware.class), deque);

        if (ReflectionUtils.isAnnotationPresent(element, ApplyMiddleware.List.class)) {
            ApplyMiddleware.List list = ReflectionUtils.retrieveRawAnnotation(element, ApplyMiddleware.List.class);

            for (ApplyMiddleware middleware : list.value())
                this.unpackMiddleware(middleware, deque);
        }
    }

    /**
     * Unpacks a {@link ApplyMiddleware} into the provided {@link Stack stack} if
     * and only if the {@link Middleware} is not globally registered and not currently
     * present in the stack.
     *
     * @param applyMiddleware The {@link ApplyMiddleware} instance to unpack.
     * @param stack           The {@link Stack stack} where the unpacked
     *                        {@link Middleware middlewares} should go.
     */
    private void unpackMiddleware(ApplyMiddleware applyMiddleware, Deque<Middleware> stack) {
        if (applyMiddleware == null) return;

        Class<? extends Middleware>[] middlewareTypes = applyMiddleware.value();
        for (Class<? extends Middleware> middlewareType : middlewareTypes) {
            if (stack.stream().map(Object::getClass).anyMatch(type -> type.equals(middlewareType)))
                continue;

            Middleware middleware = ReflectionUtils.getNewInstance(middlewareType);
            if (isRegistered(middleware)) return;
            stack.push(middleware);
        }
    }

}
