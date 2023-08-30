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
 * @see FormBody
 * @see JsonBody
 * @since 2.2.0
 */
public abstract class Body {

    /**
     * Checks if this request body is a JSON request body.
     *
     * @return {@code true} if it is a JSON request body, otherwise {@code false}.
     */
    public final boolean isJsonBody() {
        return this instanceof JsonBody;
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
     * Checks if this request body is a standard form request body.
     *
     * @return {@code true} if it is a standard form request body, otherwise {@code false}.
     */
    public final boolean isStandardFormBody() {
        return this instanceof StandardFormBody;
    }

    /**
     * Checks if this request body is a multipart form request body.
     *
     * @return {@code true} if it is a multipart form request body, otherwise {@code false}.
     */
    public final boolean isMultipartFormBody() {
        return this instanceof MultipartFormBody;
    }

}
