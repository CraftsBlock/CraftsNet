package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.events.sockets.GenericSocketEventBase;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the base for all websocket message events.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Frame
 * @see ByteBuffer
 * @see Opcode
 * @since 3.3.6-SNAPSHOT
 */
public interface GenericSocketMessageEventBase extends GenericSocketEventBase {

    /**
     * Gets the incoming message as a {@link Frame} object.
     *
     * @return The incoming message.
     */
    Frame getFrame();

    /**
     * Gets the incoming message as a {@link ByteBuffer} object.
     *
     * @return The incoming message.
     */
    default ByteBuffer getBuffer() {
        return getFrame().getBuffer();
    }

    /**
     * Gets the incoming message as a byte array.
     *
     * @return The incoming message data.
     */
    default byte @NotNull [] getData() {
        return getFrame().getData();
    }

    /**
     * Gets the incoming message as an utf8 encoded string.
     *
     * @return The incoming message.
     */
    default String getUtf8() {
        if (!getOpcode().equals(Opcode.TEXT)) return null;
        return getFrame().getUtf8();
    }

    /**
     * Gets the opcode used when sending the data.
     *
     * @return The opcode of the message.
     */
    default Opcode getOpcode() {
        return getFrame().getOpcode();
    }

}
