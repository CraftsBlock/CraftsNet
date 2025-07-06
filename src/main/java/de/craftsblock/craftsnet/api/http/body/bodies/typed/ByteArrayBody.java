package de.craftsblock.craftsnet.api.http.body.bodies.typed;

import de.craftsblock.craftsnet.api.http.Request;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link ByteArrayBody} class represents an http request body which
 * contains {@code byte[]} data.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see TypedBody
 * @since 3.5.0
 */
public final class ByteArrayBody extends TypedBody<byte[]> {

    /**
     * Constructs a new instance of a {@link TypedBody}.
     *
     * @param request The representation of the http request.
     * @param body    The body value.
     */
    public ByteArrayBody(Request request, byte @NotNull [] body) {
        super(request, body);
    }

}
