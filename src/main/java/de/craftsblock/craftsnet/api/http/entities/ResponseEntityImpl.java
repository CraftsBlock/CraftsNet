package de.craftsblock.craftsnet.api.http.entities;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Default internal implementation of {@link ResponseEntity}.
 * <p>
 * This class stores a response body and a list of transformation steps
 * that are applied before the response is written to the client.
 * <p>
 * It ensures that the response is only sent once per instance.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 1.0.0
 */
final class ResponseEntityImpl implements ResponseEntity {

    private final Object body;

    private final Collection<BiConsumer<Request, Response>> transformers = new LinkedList<>();
    private final AtomicBoolean sent = new AtomicBoolean(false);

    /**
     * Creates a new response entity wrapping the given body.
     *
     * @param body The response payload to be sent.
     */
    ResponseEntityImpl(Object body) {
        this.body = body;
    }

    /**
     * Sends the response entity through the given {@link Exchange}.
     *
     * @param exchange The HTTP exchange containing request and response objects.
     * @throws IllegalStateException If this entity has already been sent.
     */
    @Override
    public void send(Exchange exchange) {
        if (sent.compareAndSet(false, true)) {
            throw new IllegalStateException("Response entity has already been sent!");
        }

        final Request request = exchange.request();
        final Response response = exchange.response();

        for (BiConsumer<Request, Response> transformer : transformers) {
            transformer.accept(request, response);
        }

        response.print(body);
    }

    /**
     * Registers a transformation step that will be executed before sending.
     *
     * @param requestResponseBiConsumer The transformation logic.
     * @return This {@link ResponseEntity} instance for chaining.
     */
    @Override
    public ResponseEntity transform(BiConsumer<Request, Response> requestResponseBiConsumer) {
        transformers.add(requestResponseBiConsumer);
        return this;
    }

    /**
     * Returns the raw response body associated with this entity.
     *
     * @return The response payload object.
     */
    public Object getBody() {
        return body;
    }

}
