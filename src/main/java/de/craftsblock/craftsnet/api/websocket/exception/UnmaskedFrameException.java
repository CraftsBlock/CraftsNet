package de.craftsblock.craftsnet.api.websocket.exception;

import de.craftsblock.craftsnet.api.websocket.ClosureCode;

/**
 * Exception thrown when a WebSocket frame received from a client is not masked.
 * <p>
 * According to the WebSocket protocol, clients MUST mask all frames sent to a
 * server. Receiving an unmasked frame therefore constitutes a protocol violation.
 * This exception uses {@link ClosureCode#PROTOCOL_ERROR} as its associated
 * WebSocket closure code.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see FrameReadException
 * @see ClosureCode#PROTOCOL_ERROR
 * @since 4.0.0
 */
public class UnmaskedFrameException extends FrameReadException {

    /**
     * Creates a new exception indicating that a client sent an unmasked WebSocket frame.
     */
    public UnmaskedFrameException() {
        super(ClosureCode.PROTOCOL_ERROR, "A client MUST mask all frames that it sends to the server");
    }

}
