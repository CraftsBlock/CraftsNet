package de.craftsblock.craftsnet.api.middlewares;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.WebServer;

/**
 * A specific {@link Middleware middleware} for manipulating http
 * requests.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see Middleware
 * @since 3.4.0-SNAPSHOT
 */
public interface HttpMiddleware extends Middleware {

    /**
     * Defines the logic this middleware applies to the {@link HttpExchange httpExchange}.
     * <p>
     * <b>Note:</b> This method will be invoked before performing the actual
     * route logic.
     * </p>
     *
     * @param callbackInfo The {@link MiddlewareCallbackInfo callback info} that is used
     *                     to store data between middlewares.
     * @param httpExchange     The httpExchange holding the http requests data.
     */
    void handle(MiddlewareCallbackInfo callbackInfo, HttpExchange httpExchange);

    /**
     * {@inheritDoc}
     * <p>
     * This implementation tries to default all calls to
     * {@link #handle(MiddlewareCallbackInfo, HttpExchange)}.
     *
     * @param callbackInfo {@inheritDoc}
     * @param exchange     {@inheritDoc}
     */
    @Override
    default void handle(MiddlewareCallbackInfo callbackInfo, Exchange exchange) {
        if (!(exchange instanceof HttpExchange httpExchange))
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
