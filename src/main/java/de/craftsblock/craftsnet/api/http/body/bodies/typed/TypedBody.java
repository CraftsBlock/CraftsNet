package de.craftsblock.craftsnet.api.http.body.bodies.typed;

import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.Body;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link TypedBody} class represents an http request body containing
 * specific typed data.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Body
 * @since 3.4.4
 */
public abstract class TypedBody<T> extends Body {

    private final @NotNull T body;

    /**
     * Constructs a new instance of a {@link TypedBody}.
     *
     * @param request The representation of the http request.
     * @param body    The body value.
     */
    public TypedBody(Request request, @NotNull T body) {
        super(request);
        this.body = body;
    }

    /**
     * Retrieves the body data.
     *
     * @return The body data as {@link T}.
     */
    public @NotNull T getBody() {
        return body;
    }

    /**
     * Retrieves the body data type.
     *
     * @return The body data type as class instance of {@link T}.
     */
    @SuppressWarnings("unchecked")
    public @NotNull Class<T> getType() {
        return (Class<T>) body.getClass();
    }

}
