package de.craftsblock.craftsnet.api.middlewares;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.Server;

/**
 * Represents a basic {@link Middleware middleware} that can be used in context with
 * {@link Exchange exchanges} to manipulate the behaviour of the application
 * without interacting with the built-in listener system.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see MiddlewareCallbackInfo
 * @since 3.4.0-SNAPSHOT
 */
public interface Middleware {

    /**
     * Defines the logic of the middleware.
     *
     * @param callbackInfo The {@link MiddlewareCallbackInfo callback info} that is used
     *                     to store data between middlewares.
     * @param exchange     The {@link Exchange exchange} that holds the data of the request.
     */
    void handle(MiddlewareCallbackInfo callbackInfo, Exchange exchange);

    /**
     * Checks if the {@link Middleware middleware} is applicable of handling the
     * specific {@link Exchange exchange}.
     *
     * @param exchange The {@link Exchange exchange} to check.
     * @return {@code true} if the middleware can handle the exchange, {@code false}
     * otherwise.
     */
    default boolean isApplicable(Exchange exchange) {
        return isApplicable(exchange.scheme().getServerRaw());
    }

    /**
     * Checks if the {@link Middleware middleware} is applicable of handling the
     * specific {@link Server servers} exchange.
     *
     * @param server The {@link Server server} to check.
     * @return {@code true} if the middleware can handle the server, {@code false}
     * otherwise.
     */
    default boolean isApplicable(Class<? extends Server> server) {
        return true;
    }

}
