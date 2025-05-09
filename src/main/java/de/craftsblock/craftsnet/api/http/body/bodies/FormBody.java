package de.craftsblock.craftsnet.api.http.body.bodies;

import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.Body;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code FormBody} class is an abstract base class for representing HTTP request bodies that
 * contain form data. It provides common functionality for handling form data, such as deserialization
 * and field retrieval.
 *
 * @param <T> The type of data that each field in the form body holds.
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.0
 * @see Body
 * @see MultipartFormBody
 * @see StandardFormBody
 * @since 2.2.0-SNAPSHOT
 */
public abstract class FormBody<T> extends Body {

    protected final InputStream body;
    protected final ConcurrentHashMap<String, T> data = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code FormBody} with the given input stream and deserializes the form data
     * if it is of the standard form body type.
     *
     * @param request The representation of the http request.
     * @param body    The input stream containing the form data.
     * @throws IOException If an error occurs while reading or parsing the form data.
     */
    public FormBody(Request request, InputStream body) throws IOException {
        super(request);
        this.body = body;
        if (isStandardFormBody()) deserialize();
    }

    /**
     * This method should be implemented by concrete subclasses to handle the deserialization of form
     * data from the input stream and populate the internal data map.
     *
     * @throws IOException If an error occurs while reading or parsing the form data.
     */
    protected abstract void deserialize() throws IOException;

    /**
     * Clears the internal data map when the form body is closed. This is important to release resources
     * and ensure that the form body is in a clean state after processing.
     */
    @Override
    public void close() {
        data.clear();
    }

    /**
     * Checks if a specific field exists in the form data.
     *
     * @param name The name of the field to check.
     * @return {@code true} if the field exists, otherwise {@code false}.
     */
    public abstract boolean hasField(String name);

    /**
     * Retrieves the value of a specific field from the form data.
     *
     * @param name The name of the field to retrieve.
     * @return The value of the field, or {@code null} if the field does not exist.
     */
    public abstract T getField(String name);

    /**
     * Returns the raw input stream containing the form data.
     *
     * @return The input stream.
     */
    public final InputStream getBody() {
        return body;
    }

    /**
     * Returns the concurrent hash map containing the form fields and their values.
     *
     * @return The data map.
     */
    public final ConcurrentHashMap<String, T> getData() {
        return data;
    }

}
