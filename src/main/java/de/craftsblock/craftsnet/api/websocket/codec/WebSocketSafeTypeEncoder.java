package de.craftsblock.craftsnet.api.websocket.codec;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.api.codec.Encoder;
import de.craftsblock.craftsnet.api.websocket.Frame;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;

/**
 * A sealed {@link Encoder} for safely encoding types for WebSocket communication.
 * <p>
 * Implementations of this interface convert application-level objects into various
 * WebSocket compatible formats such as byte arrays, strings, {@link ByteBuffer},
 * {@link BufferUtil}, {@link de.craftsblock.craftsnet.utils.ByteBuffer}, or
 * {@link Frame frame}.
 *
 * @param <R> The result type after encoding.
 * @param <T> The input type to be encoded.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.0
 * @see TypeToBufferUtilEncoder
 * @see TypeToByteArrayEncoder
 * @see TypeToByteBufferEncoder
 * @see TypeToCraftsByteBufferEncoder
 * @see TypeToJsonEncoder
 * @see TypeToStringEncoder
 * @since 3.5.0
 */
public sealed interface WebSocketSafeTypeEncoder<R, T> extends Encoder<R, T>
        permits WebSocketSafeTypeEncoder.TypeToBufferUtilEncoder, WebSocketSafeTypeEncoder.TypeToByteArrayEncoder,
        WebSocketSafeTypeEncoder.TypeToByteBufferEncoder, WebSocketSafeTypeEncoder.TypeToCraftsByteBufferEncoder,
        WebSocketSafeTypeEncoder.TypeToJsonEncoder, WebSocketSafeTypeEncoder.TypeToStringEncoder {

    /**
     * An encoder that transforms a specific input type into a {@code byte[]} representation
     * suitable for binary WebSocket frames.
     *
     * @param <T> The type of the object to encode.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.5.0
     */
    non-sealed interface TypeToByteArrayEncoder<T> extends WebSocketSafeTypeEncoder<byte[], T> {
    }

    /**
     * An encoder that transforms a specific input type into a {@link BufferUtil} representation.
     *
     * @param <T> The type of the object to encode.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.7.0
     */
    non-sealed interface TypeToBufferUtilEncoder<T> extends WebSocketSafeTypeEncoder<BufferUtil, T> {
    }

    /**
     * An encoder that transforms a specific input type into a {@link BufferUtil} representation.
     *
     * @param <T> The type of the object to encode.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.7.0
     */
    non-sealed interface TypeToByteBufferEncoder<T> extends WebSocketSafeTypeEncoder<ByteBuffer, T> {
    }

    /**
     * An encoder that transforms a specific input type into a {@link de.craftsblock.craftsnet.utils.ByteBuffer} representation.
     *
     * @param <T> The type of the object to encode.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.5.0
     * @deprecated Deprecated in favor of {@link TypeToBufferUtilEncoder}
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "3.7.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    non-sealed interface TypeToCraftsByteBufferEncoder<T> extends WebSocketSafeTypeEncoder<de.craftsblock.craftsnet.utils.ByteBuffer, T> {
    }

    /**
     * An encoder that transforms a specific input type into a {@link Json} representation,
     * typically for use with text-based WebSocket frames.
     *
     * @param <T> The type of the object to encode.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.5.0
     */
    non-sealed interface TypeToJsonEncoder<T> extends WebSocketSafeTypeEncoder<Json, T> {
    }

    /**
     * An encoder that transforms a specific input type into a {@link String} representation,
     * typically for use with text-based WebSocket frames.
     *
     * @param <T> The type of the object to encode.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.5.0
     */
    non-sealed interface TypeToStringEncoder<T> extends WebSocketSafeTypeEncoder<String, T> {
    }

}
