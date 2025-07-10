package de.craftsblock.craftsnet.api.websocket.codec;

import de.craftsblock.craftsnet.api.codec.Decoder;
import de.craftsblock.craftsnet.api.websocket.Frame;
import org.jetbrains.annotations.ApiStatus;

/**
 * A specialized {@link Decoder} for safely decoding {@link Frame} objects into a target type.
 * <p>
 * This interface is intended for use with WebSocket based communication, where incoming frames
 * need to be deserialized or interpreted into higher-level objects while ensuring type safety.
 *
 * @param <R> The result type produced after decoding the WebSocket {@link Frame}.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.5.0
 */
@ApiStatus.Experimental
public interface WebSocketSafeTypeDecoder<R> extends Decoder<R, Frame> {

}
