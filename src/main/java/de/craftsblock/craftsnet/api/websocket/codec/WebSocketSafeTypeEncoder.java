package de.craftsblock.craftsnet.api.websocket.codec;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.api.codec.Encoder;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.ApiStatus;

/**
 * A sealed {@link Encoder} for safely encoding types for WebSocket communication.
 * <p>
 * Implementations of this interface convert application-level objects into various
 * WebSocket compatible formats such as byte arrays, strings,
 * {@link ByteBuffer byte buffers}, or {@link Frame frame}.
 *
 * @param <R> The result type after encoding.
 * @param <T> The input type to be encoded.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see TypeToByteArrayEncoder
 * @see TypeToByteBufferEncoder
 * @see TypeToJsonEncoder
 * @see TypeToStringEncoder
 * @since 3.5.0
 */
@ApiStatus.Experimental
public sealed interface WebSocketSafeTypeEncoder<R, T> extends Encoder<R, T>
        permits WebSocketSafeTypeEncoder.TypeToByteArrayEncoder, WebSocketSafeTypeEncoder.TypeToByteBufferEncoder,
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
    @ApiStatus.Experimental
    non-sealed interface TypeToByteArrayEncoder<T> extends WebSocketSafeTypeEncoder<byte[], T> {
    }

    /**
     * An encoder that transforms a specific input type into a {@link ByteBuffer} representation.
     *
     * @param <T> The type of the object to encode.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.5.0
     */
    @ApiStatus.Experimental
    non-sealed interface TypeToByteBufferEncoder<T> extends WebSocketSafeTypeEncoder<ByteBuffer, T> {
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
    @ApiStatus.Experimental
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
    @ApiStatus.Experimental
    non-sealed interface TypeToStringEncoder<T> extends WebSocketSafeTypeEncoder<String, T> {
    }

}
