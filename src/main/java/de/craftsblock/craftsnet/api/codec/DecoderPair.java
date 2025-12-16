package de.craftsblock.craftsnet.api.codec;

/**
 * A pairing between a specific target type and its associated {@link Decoder}.
 * <p>
 * This class allows registration or lookup of decoders based on the target type
 * they support.
 *
 * @param <T> The target type to be decoded.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Decoder
 * @see CodecPair
 * @since 3.5.0
 */
public final class DecoderPair<T> extends CodecPair<T, Decoder<T, ?>> {

    /**
     * Constructs a new {@link DecoderPair} for the given target type and decoder.
     *
     * @param target The type the decoder can decode to.
     * @param codec  The decoder instance.
     */
    public DecoderPair(Class<T> target, Decoder<T, ?> codec) {
        super(target, codec);
    }

}
