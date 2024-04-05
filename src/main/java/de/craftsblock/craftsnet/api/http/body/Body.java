package de.craftsblock.craftsnet.api.http.body;

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
 * @version 1.0
 * @see FormBody
 * @see JsonBody
 * @since 2.2.0
 */
public abstract class Body implements AutoCloseable {

    private boolean closed = false;

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
        return this instanceof JsonBody;
    }

    /**
     * Returns this object as a {@link JsonBody} if the body is a json object.
     *
     * @return This object as a {@link JsonBody} if the body is a json object otherwise, null.
     */
    public final JsonBody getAsJsonBody() {
        if (!isJsonBody()) return null;
        return (JsonBody) this;
    }

    /**
     * Checks if this request body is a form request body.
     *
     * @return {@code true} if it is a form request body, otherwise {@code false}.
     */
    public final boolean isFormBody() {
        return this instanceof FormBody<?>;
    }

    /**
     * Returns this object as a {@link FormBody} if the body is a form body.
     *
     * @return This object as a {@link FormBody} if the body is a form body otherwise, null.
     */
    public final FormBody<?> getAsFormBody() {
        if (!isFormBody()) return null;
        return (FormBody<?>) this;
    }

    /**
     * Checks if this request body is a standard form request body.
     *
     * @return {@code true} if it is a standard form request body, otherwise {@code false}.
     */
    public final boolean isStandardFormBody() {
        return this instanceof StandardFormBody;
    }

    /**
     * Returns this object as a {@link StandardFormBody} if the body is a standard form body.
     *
     * @return This object as a {@link StandardFormBody} if the body is a standard form body otherwise, null.
     */
    public final StandardFormBody getAsStandardFormBody() {
        if (!isStandardFormBody()) return null;
        return (StandardFormBody) this;
    }

    /**
     * Checks if this request body is a multipart form request body.
     *
     * @return {@code true} if it is a multipart form request body, otherwise {@code false}.
     */
    public final boolean isMultipartFormBody() {
        return this instanceof MultipartFormBody;
    }

    /**
     * Returns this object as a {@link MultipartFormBody} if the body is a multipart form body.
     *
     * @return This object as a {@link MultipartFormBody} if the body is a multipart form body otherwise, null.
     */
    public final MultipartFormBody getAsMultipartFormBody() {
        if (!isMultipartFormBody()) return null;
        return (MultipartFormBody) this;
    }

}
