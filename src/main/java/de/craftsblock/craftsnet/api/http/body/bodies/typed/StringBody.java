package de.craftsblock.craftsnet.api.http.body.bodies.typed;

import de.craftsblock.craftsnet.api.http.Request;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link StringBody} class represents an http request body which
 * contains {@link String} data.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see TypedBody
 * @since 3.5.0
 */
public final class StringBody extends TypedBody<String> {

    /**
     * Constructs a new instance of a {@link TypedBody}.
     *
     * @param request The representation of the http request.
     * @param body    The body value.
     */
    public StringBody(Request request, @NotNull String body) {
        super(request, body);
    }

}
