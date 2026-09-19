package de.craftsblock.craftsnet.api.http.status;

import org.jetbrains.annotations.NotNull;

/**
 * Exception that represents an HTTP-specific condition.
 * <p>
 * This exception encapsulates a {@link HttpStatus} which describes
 * the HTTP status code and reason associated with the error.
 * It can be used to propagate HTTP-related failures through the
 * application stack in a structured way.
 * <p>
 * Depending on the constructor used, a custom message and/or cause
 * can be provided. If no custom message is specified, the default
 * reason phrase of the {@link HttpStatus} will be used.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see HttpStatus
 * @since 3.7.1
 */
public class HttpStatusException extends RuntimeException {

    private final HttpStatus status;

    private boolean rawMessageFormat = false;

    /**
     * Creates a new {@link HttpStatusException} using the given status.
     * <p>
     * The exception message will default to the reason phrase of the status.
     *
     * @param status The HTTP status associated with this exception.
     */
    public HttpStatusException(@NotNull HttpStatus status) {
        this(status, status.getReason());
    }

    /**
     * Creates a new {@link HttpStatusException} with a cause.
     * <p>
     * The exception message will default to the reason phrase of the status.
     *
     * @param status The HTTP status associated with this exception.
     * @param cause  The underlying cause of this exception.
     */
    public HttpStatusException(@NotNull HttpStatus status, @NotNull Throwable cause) {
        this(status, status.getReason(), cause);
    }

    /**
     * Creates a new {@link HttpStatusException} with a custom message.
     *
     * @param status  The HTTP status associated with this exception.
     * @param message The detail message describing the error.
     */
    public HttpStatusException(@NotNull HttpStatus status, @NotNull String message) {
        super(message);
        this.status = status;
    }

    /**
     * Creates a new {@link HttpStatusException} with a custom message and cause.
     *
     * @param status  The HTTP status associated with this exception.
     * @param message The detail message describing the error.
     * @param cause   The underlying cause of this exception.
     */
    public HttpStatusException(@NotNull HttpStatus status, @NotNull String message,
                               @NotNull Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Marks this {@link HttpStatusException} as raw message.
     * The {@link de.craftsblock.craftsnet.api.http.WebHandler} will not
     * wrap the message in a custom JSON context if this is set.
     *
     * @return This {@link HttpStatusException} for chaining.
     * @since 3.7.3
     */
    public HttpStatusException asRawMessageFormat() {
        rawMessageFormat = true;
        return this;
    }

    /**
     * Checks whether this {@link HttpStatusException} is a raw message.
     * This determines if the {@link de.craftsblock.craftsnet.api.http.WebHandler WebHandler}
     * should wrap the message in a custom JSON context.
     *
     * @return {@code true} if the {@link de.craftsblock.craftsnet.api.http.WebHandler WebHandler} should
     * not wrap the message; {@code false} otherwise.
     * @since 3.7.3
     */
    public boolean isRawMessageFormat() {
        return rawMessageFormat;
    }

    /**
     * Returns the {@link HttpStatus} associated with this exception.
     *
     * @return The HTTP status.
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns the numeric HTTP status code.
     *
     * @return The HTTP status code.
     */
    public int getCode() {
        return status.getCode();
    }

}