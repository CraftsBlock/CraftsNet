package de.craftsblock.craftsnet.api.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Enumeration representing WebSocket closure codes along with their integer values and internal status.
 * These codes are defined in RFC 6455 (<a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1</a>).
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.5-SNAPSHOT
 */
public enum ClosureCode {

    /**
     * Indicates a normal closure, meaning that the purpose for which the connection was established has been fulfilled.
     */
    NORMAL(1000, false),

    /**
     * Indicates that an endpoint is "going away", such as a server going down or a browser having navigated away from a page.
     */
    GOING_AWAY(1001, false),

    /**
     * Indicates a protocol error, meaning that the endpoint received a message that violates its protocol.
     */
    PROTOCOL_ERROR(1002, true),

    /**
     * Indicates that the received message type is unsupported or not expected.
     */
    UNSUPPORTED(1003, true),

    /**
     * Indicates that no status code was actually present.
     */
    NO_STATUS(1005, true),

    /**
     * Indicates an abnormal closure, meaning that the connection was closed abruptly without sending or receiving a close frame.
     */
    ABNORMAL(1006, true),

    /**
     * Indicates that the endpoint received a message with a payload that it cannot accept.
     */
    UNSUPPORTED_PAYLOAD(1007, true),

    /**
     * Indicates a violation of the WebSocket protocol's policy.
     */
    POLICY_VIOLATION(1008, false),

    /**
     * Indicates that the message received was too large for the endpoint to process.
     */
    TOO_LARGE(1009, false),

    /**
     * Indicates that the server is terminating the connection because it encountered an unexpected condition that prevented it from fulfilling the request.
     */
    SERVER_ERROR(1011, false),

    /**
     * Indicates that the service is terminating the connection because it is restarting.
     */
    SERVICE_ERROR(1012, false),

    /**
     * Indicates that the service is terminating the connection because it encountered an error during the transmission of the response.
     */
    TRY_AGAIN_LATER(1013, false),

    /**
     * Indicates that the server is terminating the connection because it received a message that was too large.
     */
    BAD_GATEWAY(1014, false),

    /**
     * Indicates that the connection was closed due to a failure to perform a TLS handshake.
     */
    TLS_HANDSHAKE_FAIL(1015, true);

    /**
     * List of ClosureCode values that are considered internal.
     */
    public static final List<ClosureCode> INTERNAL_CODES;

    /**
     * List of integer values corresponding to ClosureCode values that are considered internal.
     */
    public static final List<Integer> RAW_INTERNAL_CODES;

    static {
        List<ClosureCode> internal_codes = new ArrayList<>();
        for (ClosureCode closureCode : ClosureCode.values())
            if (closureCode.isInternal()) internal_codes.add(closureCode);
        INTERNAL_CODES = Collections.unmodifiableList(internal_codes);

        RAW_INTERNAL_CODES = INTERNAL_CODES.parallelStream().map(ClosureCode::intValue).toList();
    }

    private final int code;
    private final boolean internal;

    /**
     * Constructs a ClosureCode with the specified integer value and internal status.
     *
     * @param code     The integer value representing the closure code.
     * @param internal Indicates whether the closure code is considered internal.
     */
    ClosureCode(int code, boolean internal) {
        this.code = code;
        this.internal = internal;
    }

    /**
     * Retrieves the integer value of the ClosureCode.
     *
     * @return The integer value representing the closure code.
     */
    public int intValue() {
        return code;
    }

    /**
     * Checks whether the ClosureCode is considered internal.
     *
     * @return true if the ClosureCode is considered internal, otherwise false.
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * Converts an int to the corresponding {@link ClosureCode}.
     *
     * @param code The int which should be converted.
     * @return The corresponding {@link ClosureCode}.
     */
    public static ClosureCode fromInt(int code) {
        for (ClosureCode closureCode : ClosureCode.values())
            if (closureCode.intValue() == code)
                return closureCode;
        return null;
    }

}
