package de.craftsblock.craftsnet.api.middlewares;

import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.middlewares.annotation.ApplyMiddleware;
import de.craftsblock.craftsnet.utils.ReflectionUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * A registry for resolving and storing {@link Middleware middlewares}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Middleware
 * @since 3.4.0-SNAPSHOT
 */
public class MiddlewareRegistry {

    private final Stack<Middleware> middlewares = new Stack<>();

    /**
     * Registers a {@link Middleware middleware} that should be applied
     * globally.
     *
     * @param middleware The {@link Middleware middleware} to register.
     */
    public void register(Middleware middleware) {
        if (isRegistered(middleware)) return;
        middlewares.add(middleware);
    }

    /**
     * Unregisters a {@link Middleware middleware} that should no longer
     * be applied globally.
     *
     * @param middleware The {@link Middleware middleware} to register.
     */
    public void unregister(Middleware middleware) {
        middlewares.remove(middleware);
    }

    /**
     * Checks if a specific {@link Middleware middleware} is registered
     * for global appliance.
     *
     * @param middleware The {@link Middleware middleware} to check.
     * @return {@code true} if it is register for global appliance, {@code false} otherwise.
     */
    public boolean isRegistered(Middleware middleware) {
        return middlewares.stream()
                .map(Object::getClass)
                .anyMatch(type -> type.equals(middleware.getClass()));
    }

    /**
     * Get all global applied {@link Middleware middlewares}.
     *
     * @return A {@link Stack stack} of {@link Middleware middlewares}for global appliance.
     */
    public Stack<Middleware> getMiddlewares() {
        return middlewares;
    }

    /**
     * Get all, for the exchange applicable, global applied
     * {@link Middleware middlewares}.
     *
     * @param exchange The exchange for which the {@link Middleware middlewares}
     *                 should be applicable.
     * @return The {@link Stack stack} of {@link Middleware middlewares} for global appliance.
     */
    public Stack<Middleware> getMiddlewares(BaseExchange exchange) {
        return getMiddlewares(exchange.scheme().getServerRaw());
    }

    /**
     * Get all, for the {@link Server server} type applicable, global
     * applied {@link Middleware middlewares}.
     *
     * @param server The {@link Server server} type for which the
     *               {@link Middleware middlewares} should be applicable.
     * @return The {@link Stack stack} of {@link Middleware middlewares} for global appliance.
     */
    public Stack<Middleware> getMiddlewares(Class<? extends Server> server) {
        return middlewares.stream().filter(middleware -> middleware.isApplicable(server)).collect(Collectors.toCollection(Stack::new));
    }

    /**
     * Retrieves a {@link Stack stack} of {@link Middleware middlewares}
     * from a specific {@link Handler endpoint handler} nd its child
     * {@link Method method}.
     *
     * @param root    The {@link Handler endpoint handler}.
     * @param handler The {@link Method method}.
     * @return A {@link Stack stack} of {@link Middleware middlewares} that are present.
     */
    public Stack<Middleware> resolveMiddlewares(Handler root, Method handler) {
        Stack<Middleware> middlewareStack = new Stack<>();

        Class<? extends Handler> type = root.getClass();
        this.resolveMiddlewares(type, middlewareStack);
        this.resolveMiddlewares(handler, middlewareStack);

        return middlewareStack.stream().collect(Collectors.toCollection(Stack::new));
    }

    /**
     * Retrieves a {@link Stack stack} of {@link Middleware middlewares}
     * from a {@link AnnotatedElement} and push it into an existing stack.
     *
     * @param element The {@link AnnotatedElement} to search on.
     * @param stack   The {@link Stack stack} where the result should be pushed to.
     */
    private void resolveMiddlewares(AnnotatedElement element, Stack<Middleware> stack) {
        this.unpackMiddleware(ReflectionUtils.retrieveRawAnnotation(element, ApplyMiddleware.class), stack);

        if (ReflectionUtils.isAnnotationPresent(element, ApplyMiddleware.List.class)) {
            ApplyMiddleware.List list = ReflectionUtils.retrieveRawAnnotation(element, ApplyMiddleware.List.class);

            for (ApplyMiddleware middleware : list.value())
                this.unpackMiddleware(middleware, stack);
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
    private void unpackMiddleware(ApplyMiddleware applyMiddleware, Stack<Middleware> stack) {
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
