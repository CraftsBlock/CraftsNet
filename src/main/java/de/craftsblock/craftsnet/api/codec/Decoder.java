package de.craftsblock.craftsnet.api.codec;

import org.jetbrains.annotations.ApiStatus;

/**
 * A generic interface for decoding data from one type into another.
 * <p>
 * Implementations of this interface are responsible for converting encoded data
 * into their corresponding decoded representation.
 *
 * @param <R> The result type after decoding.
 * @param <T> The input type to be decoded.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.5.0
 */
@ApiStatus.Experimental
public non-sealed interface Decoder<R, T> extends Codec<R, T> {

    /**
     * Decodes the given input into its corresponding output representation.
     *
     * @param t The input to decode.
     * @return The decoded result.
     */
    R decode(T t);

}
