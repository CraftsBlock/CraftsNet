package de.craftsblock.craftsnet.api.http.exception;

import de.craftsblock.craftsnet.api.http.status.HttpStatus;
import de.craftsblock.craftsnet.api.http.status.HttpStatusException;

/**
 * Exception thrown when an HTTP request body exceeds the maximum permitted size.
 * <p>
 * This is an unchecked exception that can be used to indicate that the request
 * body cannot be processed because it is too large.
 */
public class RequestBodyTooLargeException extends HttpStatusException {

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message the detail message describing why the request body is too large
     */
    public RequestBodyTooLargeException(String message) {
        super(HttpStatus.ClientError.PAYLOAD_TOO_LARGE, message);
    }

}
