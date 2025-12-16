package de.craftsblock.craftsnet.events.sockets.message;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.events.sockets.GenericSocketEventBase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Represents the base for all websocket message events.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.0
 * @see Frame
 * @see ByteBuffer
 * @see Opcode
 * @since 3.4.0-SNAPSHOT
 */
public interface GenericSocketMessageEventBase extends GenericSocketEventBase {

    /**
     * Gets the incoming message as a {@link Frame} object.
     *
     * @return The incoming message.
     */
    Frame getFrame();

    /**
     * Gets the incoming message as a {@link de.craftsblock.craftsnet.utils.ByteBuffer} object.
     *
     * @return The incoming message.
     * @deprecated in favor of {@link #getByteBuffer()} and {@link #getBufferUtil()}
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "3.7.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    default de.craftsblock.craftsnet.utils.ByteBuffer getBuffer() {
        return getFrame().getBuffer();
    }

    /**
     * Gets the message as a {@link BufferUtil} object.
     *
     * @return The message.
     * @since 3.7.0
     */
    default BufferUtil getBufferUtil() {
        return getFrame().getBufferUtil();
    }

    /**
     * Gets the message as a {@link ByteBuffer} object.
     *
     * @return The message.
     * @since 3.7.0
     */
    default ByteBuffer getByteBuffer() {
        return getBufferUtil().getRaw();
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
