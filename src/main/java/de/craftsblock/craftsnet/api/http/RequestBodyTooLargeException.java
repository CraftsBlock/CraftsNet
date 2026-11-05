package de.craftsblock.craftsnet.api.http;

/**
 * Exception thrown when an HTTP request body exceeds the maximum permitted size.
 * <p>
 * This is an unchecked exception that can be used to indicate that the request
 * body cannot be processed because it is too large.
 */
public class RequestBodyTooLargeException extends RuntimeException {

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message the detail message describing why the request body is too large
     */
    public RequestBodyTooLargeException(String message) {
        super(message);
    }

}
