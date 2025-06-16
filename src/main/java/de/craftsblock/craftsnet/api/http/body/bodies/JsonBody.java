package de.craftsblock.craftsnet.api.http.body.bodies;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.bodies.typed.TypedBody;

/**
 * The {@link JsonBody} class represents an http request body which
 * contains {@link Json} data.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.1.0
 * @see TypedBody
 * @since 2.2.0-SNAPSHOT
 */
public final class JsonBody extends TypedBody<Json> {

    /**
     * Constructs a new {@link JsonBody} object.
     *
     * @param request The representation of the http request.
     * @param body    The body content as {@link Json}.
     */
    public JsonBody(Request request, Json body) {
        super(request, body);
    }

}
