package de.craftsblock.craftsnet.api.middlewares;

import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.WebServer;

/**
 * A specific {@link Middleware middleware} for manipulating http
 * requests.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Middleware
 * @since 3.4.0-SNAPSHOT
 */
public interface HttpMiddleware extends Middleware {

    /**
     * Defines the logic this middleware applies to the {@link Exchange exchange}.
     * <p>
     * <b>Note:</b> This method will be invoked before performing the actual
     * route logic.
     * </p>
     *
     * @param callbackInfo The {@link MiddlewareCallbackInfo callback info} that is used
     *                     to store data between middlewares.
     * @param exchange     The exchange holding the http requests data.
     */
    void handle(MiddlewareCallbackInfo callbackInfo, Exchange exchange);

    /**
     * {@inheritDoc}
     * <p>
     * This implementation tries to default all calls to
     * {@link #handle(MiddlewareCallbackInfo, Exchange)}.
     *
     * @param callbackInfo {@inheritDoc}
     * @param exchange     {@inheritDoc}
     */
    @Override
    default void handle(MiddlewareCallbackInfo callbackInfo, BaseExchange exchange) {
        if (!(exchange instanceof Exchange httpExchange))
            throw new IllegalStateException("Http middleware may not be called with an " + exchange.getClass().getSimpleName() + " exchange!");
        this.handle(callbackInfo, httpExchange);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation checks if the server is a {@link WebServer web server}.
     *
     * @param server {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    default boolean isApplicable(Class<? extends Server> server) {
        return WebServer.class.isAssignableFrom(server);
    }

}
