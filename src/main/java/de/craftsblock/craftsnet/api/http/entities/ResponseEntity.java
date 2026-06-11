package de.craftsblock.craftsnet.api.http.entities;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents a response abstraction that can be sent through an {@link Exchange}.
 * <p>
 * A {@code ResponseEntity} encapsulates a response body together with optional
 * transformation steps that can modify the {@link Request} and {@link Response}
 * before the final output is written.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.7.3
 */
public interface ResponseEntity {

    /**
     * Sends this response entity through the given {@link Exchange}.
     *
     * @param exchange The HTTP exchange containing request and response objects.
     */
    void send(Exchange exchange);

    /**
     * Adds a transformation step that operates only on the {@link Response}.
     * <p>
     * This is a convenience method for transformations that do not require
     * access to the {@link Request}.
     *
     * @param responseConsumer The consumer modifying the response.
     * @return This {@link ResponseEntity} instance for chaining.
     */
    default ResponseEntity transform(Consumer<Response> responseConsumer) {
        return transform((request, response) -> responseConsumer.accept(response));
    }

    /**
     * Adds a transformation step that can access both the {@link Request}
     * and {@link Response}.
     *
     * @param requestResponseBiConsumer The transformation logic.
     * @return This {@link ResponseEntity} instance for chaining.
     */
    ResponseEntity transform(BiConsumer<Request, Response> requestResponseBiConsumer);

    /**
     * Creates a simple {@link ResponseEntity} with the given response body.
     *
     * @param response The response body to send.
     * @return A new {@link ResponseEntity} instance wrapping the value.
     */
    static ResponseEntity simple(Object response) {
        return new ResponseEntityImpl(response);
    }

}
