package de.craftsblock.craftsnet.api.http;

import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an HTTP status code according to the HTTP specification (RFC 9110).
 *
 * <p>Concrete status codes are implemented using nested enums corresponding to
 * their category:</p>
 * <ul>
 *     <li>{@link Info} – Informational (1xx)</li>
 *     <li>{@link Success} – Success (2xx)</li>
 *     <li>{@link Redirection} – Redirection (3xx)</li>
 *     <li>{@link ClientError} – Client Error (4xx)</li>
 *     <li>{@link ServerError} – Server Error (5xx)</li>
 * </ul>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.7.0
 */
public sealed interface HttpStatus
        permits HttpStatus.Info, HttpStatus.Success, HttpStatus.Redirection,
        HttpStatus.ClientError, HttpStatus.ServerError {

    /**
     * Immutable lookup map for retrieving HttpStatus instances by their numeric code.
     */
    Map<Integer, HttpStatus> LOOKUP = Stream.of(
                    Info.values(),
                    Success.values(),
                    Redirection.values(),
                    ClientError.values(),
                    ServerError.values()
            ).flatMap(Arrays::stream)
            .collect(Collectors.toUnmodifiableMap(
                    HttpStatus::getCode,
                    Function.identity()
            ));

    /**
     * Returns the HTTP reason phrase for this status.
     *
     * @return The reason phrase.
     */
    String getReason();

    /**
     * Returns the numeric HTTP status code.
     *
     * @return The numeric code.
     */
    int getCode();

    /**
     * Generates a full HTTP status line for this status.
     *
     * @param protocol The HTTP protocol version.
     * @return A string in the format "HTTP/1.1 CODE REASON\r\n"
     */
    default String getStatusLine(String protocol) {
        return protocol + " " + getCode() + " " + getReason() + "\r\n";
    }

    /**
     * Returns the category of this status code as the first digit.
     *
     * @return The category corresponding to the status code class.
     */
    default @Range(from = 1, to = 5) int getCategory() {
        return getCode() / 100;
    }

    /**
     * Checks whether this status belongs to the given category.
     *
     * @param category The category number.
     * @return {@code true} if the status code belongs to this category.
     */
    default boolean isCategory(int category) {
        return getCategory() == category;
    }

    /**
     * Checks whether this status is informational.
     *
     * @return {@code true} if the status code is informational (1xx).
     */
    default boolean isInfo() {
        return isCategory(1);
    }

    /**
     * Checks whether this status indicates success.
     *
     * @return {@code true} if the status code indicates success (2xx).
     */
    default boolean isSuccess() {
        return isCategory(2);
    }

    /**
     * Checks whether this status indicates redirection.
     *
     * @return {@code true} if the status code indicates redirection (3xx).
     */
    default boolean isRedirect() {
        return isCategory(3);
    }

    /**
     * Checks whether this status indicates a client error.
     *
     * @return {@code true} if the status code indicates a client error (4xx).
     */
    default boolean isClientError() {
        return isCategory(4);
    }

    /**
     * Checks whether this status indicates a server error.
     *
     * @return {@code true} if the status code indicates a server error (5xx).
     */
    default boolean isServerError() {
        return isCategory(5);
    }

    /**
     * Checks if a numeric code is a valid HTTP status code.
     *
     * @param code The numeric code
     * @return {@code true} if the code is between 100 and 599 (inclusive)
     */
    static boolean isValid(int code) {
        return code >= 100 && code < 600;
    }

    /**
     * Retrieves the {@link HttpStatus} instance corresponding to the given code.
     *
     * @param code The numeric HTTP status code
     * @return The {@link HttpStatus} instance, or {@code null} if the code is not defined
     */
    static HttpStatus fromCode(int code) {
        return LOOKUP.get(code);
    }

    /**
     * HTTP 1xx informational response codes.
     */
    enum Info implements HttpStatus {

        /**
         * 100 Continue
         */
        CONTINUE(100, "Continue"),

        /**
         * 101 Switching Protocols
         */
        SWITCHING_PROTOCOLS(101, "Switching Protocols"),

        /**
         * 102 Processing
         */
        PROCESSING(102, "Processing"),

        /**
         * 103 Early Hints
         */
        EARLY_HINTS(103, "Early Hints"),
        ;

        private final int code;
        private final String reason;

        /**
         * Constructs an {@link Info} with the specified code and reason.
         *
         * @param code   The HTTP status code.
         * @param reason The HTTP status reason phrase.
         */
        Info(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public String getReason() {
            return reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public int getCode() {
            return code;
        }

    }

    /**
     * HTTP 2xx success response codes.
     */
    enum Success implements HttpStatus {

        /**
         * 200 OK
         */
        OK(200, "OK"),

        /**
         * 201 Created
         */
        CREATED(201, "Created"),

        /**
         * 202 Accepted
         */
        ACCEPTED(202, "Accepted"),

        /**
         * 203 Non-Authoritative Information
         */
        NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),

        /**
         * 204 No Content
         */
        NO_CONTENT(204, "No Content"),

        /**
         * 205 Reset Content
         */
        RESET_CONTENT(205, "Reset Content"),

        /**
         * 206 Partial Content
         */
        PARTIAL_CONTENT(206, "Partial Content"),

        /**
         * 207 Multi-Status
         */
        MULTI_STATUS(207, "Multi-Status"),

        /**
         * 208 Already Reported
         */
        ALREADY_REPORTED(208, "Already Reported"),

        /**
         * 226 IM Used
         */
        IM_USED(226, "IM Used"),
        ;

        private final int code;
        private final String reason;

        /**
         * Constructs an {@link Success} with the specified code and reason.
         *
         * @param code   The HTTP status code.
         * @param reason The HTTP status reason phrase.
         */
        Success(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public String getReason() {
            return reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public int getCode() {
            return code;
        }

    }

    /**
     * HTTP 3xx redirection response codes.
     */
    enum Redirection implements HttpStatus {

        /**
         * 300 Multiple Choices
         */
        MULTIPLE_CHOICES(300, "Multiple Choices"),

        /**
         * 301 Moved Permanently
         */
        MOVED_PERMANENTLY(301, "Moved Permanently"),

        /**
         * 302 Found
         */
        FOUND(302, "Found"),

        /**
         * 303 See Other
         */
        SEE_OTHER(303, "See Other"),

        /**
         * 304 Not Modified
         */
        NOT_MODIFIED(304, "Not Modified"),

        /**
         * 305 Use Proxy
         */
        USE_PROXY(305, "Use Proxy"),

        /**
         * 307 Temporary Redirect
         */
        TEMPORARY_REDIRECT(307, "Temporary Redirect"),

        /**
         * 308 Permanent Redirect
         */
        PERMANENT_REDIRECT(308, "Permanent Redirect"),
        ;

        private final int code;
        private final String reason;

        /**
         * Constructs an {@link Redirection} with the specified code and reason.
         *
         * @param code   The HTTP status code.
         * @param reason The HTTP status reason phrase.
         */
        Redirection(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public String getReason() {
            return reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public int getCode() {
            return code;
        }

    }

    /**
     * HTTP 4xx client error response codes.
     */
    enum ClientError implements HttpStatus {

        /**
         * 400 Bad Request
         */
        BAD_REQUEST(400, "Bad Request"),

        /**
         * 401 Unauthorized
         */
        UNAUTHORIZED(401, "Unauthorized"),

        /**
         * 402 Payment Required
         */
        PAYMENT_REQUIRED(402, "Payment Required"),

        /**
         * 403 Forbidden
         */
        FORBIDDEN(403, "Forbidden"),

        /**
         * 404 Not Found
         */
        NOT_FOUND(404, "Not Found"),

        /**
         * 405 Method Not Allowed
         */
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

        /**
         * 406 Not Acceptable
         */
        NOT_ACCEPTABLE(406, "Not Acceptable"),

        /**
         * 407 Proxy Authentication Required
         */
        PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),

        /**
         * 408 Request Timeout
         */
        REQUEST_TIMEOUT(408, "Request Timeout"),

        /**
         * 409 Conflict
         */
        CONFLICT(409, "Conflict"),

        /**
         * 410 Gone
         */
        GONE(410, "Gone"),

        /**
         * 411 Length Required
         */
        LENGTH_REQUIRED(411, "Length Required"),

        /**
         * 412 Precondition Failed
         */
        PRECONDITION_FAILED(412, "Precondition Failed"),

        /**
         * 413 Payload Too Large
         */
        PAYLOAD_TOO_LARGE(413, "Payload Too Large"),

        /**
         * 414 URI Too Long
         */
        URI_TOO_LONG(414, "URI Too Long"),

        /**
         * 415 Unsupported Media Type
         */
        UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

        /**
         * 416 Range Not Satisfiable
         */
        RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),

        /**
         * 417 Expectation Failed
         */
        EXPECTATION_FAILED(417, "Expectation Failed"),

        /**
         * 421 Misdirected Request
         */
        MISDIRECTED_REQUEST(421, "Misdirected Request"),

        /**
         * 422 Unprocessable Entity
         */
        UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),

        /**
         * 423 Locked
         */
        LOCKED(423, "Locked"),

        /**
         * 424 Failed Dependency
         */
        FAILED_DEPENDENCY(424, "Failed Dependency"),

        /**
         * 425 Too Early
         */
        TOO_EARLY(425, "Too Early"),

        /**
         * 426 Upgrade Required
         */
        UPGRADE_REQUIRED(426, "Upgrade Required"),

        /**
         * 428 Precondition Required
         */
        PRECONDITION_REQUIRED(428, "Precondition Required"),

        /**
         * 429 Too Many Requests
         */
        TOO_MANY_REQUESTS(429, "Too Many Requests"),

        /**
         * 431 Request Header Fields Too Large
         */
        REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),

        /**
         * 451 Unavailable For Legal Reasons
         */
        UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),
        ;

        private final int code;
        private final String reason;

        /**
         * Constructs an {@link ClientError} with the specified code and reason.
         *
         * @param code   The HTTP status code.
         * @param reason The HTTP status reason phrase.
         */
        ClientError(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public String getReason() {
            return reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public int getCode() {
            return code;
        }

    }

    /**
     * HTTP 5xx server error response codes.
     */
    enum ServerError implements HttpStatus {

        /**
         * 500 Internal Server Error
         */
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),

        /**
         * 501 Not Implemented
         */
        NOT_IMPLEMENTED(501, "Not Implemented"),

        /**
         * 502 Bad Gateway
         */
        BAD_GATEWAY(502, "Bad Gateway"),

        /**
         * 503 Service Unavailable
         */
        SERVICE_UNAVAILABLE(503, "Service Unavailable"),

        /**
         * 504 Gateway Timeout
         */
        GATEWAY_TIMEOUT(504, "Gateway Timeout"),

        /**
         * 505 HTTP Version Not Supported
         */
        HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),

        /**
         * 506 Variant Also Negotiates
         */
        VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),

        /**
         * 507 Insufficient Storage
         */
        INSUFFICIENT_STORAGE(507, "Insufficient Storage"),

        /**
         * 508 Loop Detected
         */
        LOOP_DETECTED(508, "Loop Detected"),

        /**
         * 510 Not Extended
         */
        NOT_EXTENDED(510, "Not Extended"),

        /**
         * 511 Network Authentication Required
         */
        NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

        private final int code;
        private final String reason;

        /**
         * Constructs an {@link ServerError} with the specified code and reason.
         *
         * @param code   The HTTP status code.
         * @param reason The HTTP status reason phrase.
         */
        ServerError(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public String getReason() {
            return reason;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public int getCode() {
            return code;
        }

    }

}
