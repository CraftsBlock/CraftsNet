package de.craftsblock.craftsnet.api.codec;

import org.jetbrains.annotations.ApiStatus;

/**
 * A generic base interface for codecs, combining encoding and decoding operations.
 * <p>
 * This interface serves as a marker or contract for implementations that either encode,
 * decode, or do both.
 *
 * @param <R> The result type (encoded or decoded).
 * @param <T> The input type.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Encoder
 * @see Decoder
 * @since 3.5.0
 */
@ApiStatus.Experimental
@SuppressWarnings("unused")
public sealed interface Codec<R, T> permits Decoder, Encoder {
}
