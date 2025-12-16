package de.craftsblock.craftsnet.api.codec;

/**
 * A generic interface for encoding data from one type into another.
 * <p>
 * Implementations of this interface are responsible for converting objects
 * into an encoded format.
 *
 * @param <R> The result type after encoding.
 * @param <T> The input type to be encoded.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.5.0
 */
public non-sealed interface Encoder<R, T> extends Codec<R, T> {

    /**
     * Encodes the given input into its corresponding encoded representation.
     *
     * @param t The input to encode.
     * @return The encoded result.
     */
    R encode(T t);

}
