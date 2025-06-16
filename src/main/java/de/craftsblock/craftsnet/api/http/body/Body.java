package de.craftsblock.craftsnet.api.http.body;

import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.bodies.FormBody;
import de.craftsblock.craftsnet.api.http.body.bodies.JsonBody;
import de.craftsblock.craftsnet.api.http.body.bodies.MultipartFormBody;
import de.craftsblock.craftsnet.api.http.body.bodies.StandardFormBody;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The abstract class {@code Body} is the base class for all types of HTTP request bodies
 * supported by CraftsNet.
 * <p>
 * An HTTP request body contains data that is sent to the server when making an HTTP request.
 * This abstract class serves to provide common properties and methods for different types of
 * request bodies.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.0
 * @see FormBody
 * @see JsonBody
 * @since 2.2.0-SNAPSHOT
 */
public abstract class Body implements AutoCloseable {

    private static final ConcurrentHashMap<Request, ConcurrentHashMap<Class<? extends Body>, Body>> bodies = new ConcurrentHashMap<>();

    private final Request request;

    private boolean closed = false;

    /**
     * Constructs a new instance of a request body.
     *
     * @param request The representation of the http request.
     */
    public Body(Request request) {
        this.request = request;
        bodies.computeIfAbsent(request, b -> new ConcurrentHashMap<>()).put(getClass(), this);
    }

    /**
     * <p>Returns this content type of the underlying request.</p>
     * <b>Important:</b> This method does not return the exact content type of the inputted data! It relies on
     * the request to retrieve the estimated content type.
     *
     * @return The content type retrieved by the request.
     * @since 3.0.4-SNAPSHOT
     */
    public final String getRawContentType() {
        return this.request.getContentType().replace("\"", "");
    }

    /**
     * Closes the request body.
     */
    @Override
    public void close() {
        closed = true;
    }

    /**
     * Checks if this request body is already closed.
     *
     * @return {@code true} if it is closed, otherwise {@code false}.
     */
    public final boolean isClosed() {
        return closed;
    }

    /**
     * Checks if this request body is a JSON request body.
     *
     * @return {@code true} if it is a JSON request body, otherwise {@code false}.
     */
    public final boolean isJsonBody() {
        return isBodyFromType(JsonBody.class);
    }

    /**
     * Returns this object as a {@link JsonBody} if the body is a json object.
     *
     * @return This object as a {@link JsonBody} if the body is a json object otherwise, null.
     */
    public final JsonBody getAsJsonBody() {
        return getAsType(JsonBody.class);
    }

    /**
     * Checks if this request body is a standard form request body.
     *
     * @return {@code true} if it is a standard form request body, otherwise {@code false}.
     */
    public final boolean isStandardFormBody() {
        return isBodyFromType(StandardFormBody.class);
    }

    /**
     * Returns this object as a {@link StandardFormBody} if the body is a standard form body.
     *
     * @return This object as a {@link StandardFormBody} if the body is a standard form body otherwise, null.
     */
    public final StandardFormBody getAsStandardFormBody() {
        return getAsType(StandardFormBody.class);
    }

    /**
     * Checks if this request body is a multipart form request body.
     *
     * @return {@code true} if it is a multipart form request body, otherwise {@code false}.
     */
    public final boolean isMultipartFormBody() {
        return isBodyFromType(MultipartFormBody.class);
    }

    /**
     * Returns this object as a {@link MultipartFormBody} if the body is a multipart form body.
     *
     * @return This object as a {@link MultipartFormBody} if the body is a multipart form body otherwise, null.
     */
    public final MultipartFormBody getAsMultipartFormBody() {
        return getAsType(MultipartFormBody.class);
    }

    /**
     * Checks if this request body is a specific type of request body.
     * <p><b>Important:</b> As it is only a preview it may change in a future release.</p>
     *
     * @param type The type which should be checked.
     * @return true if the type is the type of the current request body, false otherwise.
     */
    public final boolean isBodyFromType(Class<? extends Body> type) {
        if (closed) throw new IllegalStateException("Can not check the body type as it is already closed!");
        if (bodies.getOrDefault(this.request, new ConcurrentHashMap<>()).containsKey(type)) return true;

        // Check if a body parser exists which could parse to the expected type.
        BodyRegistry registry = this.request.getCraftsNet().bodyRegistry();
        if (!registry.isParserPresent(type)) return false;
        return registry.getParser(type).isParseable(getRawContentType().split(";")[0]);
    }

    /**
     * Returns this object as a specific type of request body if the body is an instance of the specific request body.
     * <p><b>Important:</b> As it is only a preview it may change in a future release.</p>
     *
     * @param type The type which the current request body should be cast to.
     * @param <T>  The type of the request body.
     * @return The object after casting, or null if the current request body is not an instance of the type.
     */
    public final <T extends Body> @Nullable T getAsType(Class<T> type) {
        if (closed) throw new IllegalStateException("Can not cast the body as it is already closed!");
        ConcurrentHashMap<Class<? extends Body>, Body> bodies = Body.bodies.getOrDefault(this.request, new ConcurrentHashMap<>());
        if (isBodyFromType(type) && bodies.containsKey(type)) return type.cast(bodies.get(type));

        try {
            // Try to parse the body to the wanted type if it's not present
            BodyRegistry registry = this.request.getCraftsNet().bodyRegistry();
            if (!registry.isParserPresent(type)) return null;
            try (InputStream stream = this.request.getRawBody()) {
                return this.request.getCraftsNet().bodyRegistry().getParser(type).parse(this.request, stream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a set containing all the valid types this body was parsed for.
     * <p><b>Important:</b> As it is only a preview it may change in a future release.</p>
     *
     * @return The set containing all body types as classes.
     */
    public final Set<Class<? extends Body>> getBodyTypes() {
        return getBodies().keySet();
    }

    /**
     * Returns an {@link ConcurrentHashMap} containing all parsed bodies for the underlying request. The key is the type of the body as it's class
     * representation.
     * <p><b>Important:</b> As it is only a preview it may change in a future release.</p>
     *
     * @return The {@link ConcurrentHashMap} containing all parsed bodies.
     */
    public final ConcurrentHashMap<Class<? extends Body>, Body> getBodies() {
        if (closed) throw new IllegalStateException("Can not access the bodies as they are already closed!");
        return bodies.get(this.request);
    }

    /**
     * Closes all bodies corresponding to a specific request.
     * <p><b>Important:</b> As it is only a preview it may change in a future release.</p>
     *
     * @param request The request which should be cleaned.
     */
    @ApiStatus.Internal
    public static void cleanUp(Request request) {
        if (!bodies.containsKey(request)) return;
        bodies.remove(request).values().forEach(Body::close);
    }

}
