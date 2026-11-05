package de.craftsblock.craftsnet.api.websocket.exception;

import de.craftsblock.craftsnet.api.websocket.ClosureCode;

/**
 * Exception thrown when a WebSocket frame exceeds the maximum permitted size.
 * <p>
 * This exception uses {@link ClosureCode#TOO_LARGE} as its associated
 * WebSocket closure code to indicate that the received message is too large.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see FrameReadException
 * @see ClosureCode#TOO_LARGE
 * @since 4.0.0
 */
public class MaxFrameSizeExceededException extends FrameReadException {

    /**
     * Creates a new exception indicating that the maximum permitted frame size
     * has been exceeded.
     */
    public MaxFrameSizeExceededException() {
        super(ClosureCode.TOO_LARGE, "Message too large");
    }

}
