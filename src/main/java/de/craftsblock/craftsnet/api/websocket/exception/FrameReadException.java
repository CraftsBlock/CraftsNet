package de.craftsblock.craftsnet.api.websocket.exception;

import de.craftsblock.craftsnet.api.websocket.ClosureCode;

/**
 * Exception thrown when reading a WebSocket frame fails.
 * <p>
 * This exception carries a {@link ClosureCode} that indicates the WebSocket
 * close code associated with the frame read failure.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 4.0.0
 */
public class FrameReadException extends Exception {

    private final ClosureCode closureCode;

    /**
     * Creates a new frame read exception with the specified closure code and message.
     *
     * @param closureCode the WebSocket closure code associated with the failure
     * @param message     the detail message describing the failure
     */
    public FrameReadException(ClosureCode closureCode, String message) {
        super(message);

        this.closureCode = closureCode;
    }

    /**
     * Returns the WebSocket closure code associated with this exception.
     *
     * @return the closure code associated with the frame read failure
     */
    public ClosureCode getClosureCode() {
        return closureCode;
    }

}
