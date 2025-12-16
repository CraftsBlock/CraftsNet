package de.craftsblock.craftsnet.api.codec;

/**
 * A pairing between a specific target type and its associated {@link Encoder}.
 * <p>
 * This class allows registration or lookup of encoders based on the target type
 * they support.
 *
 * @param <T> The target type to be encoded.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Encoder
 * @see CodecPair
 * @since 3.5.0
 */
public final class EncoderPair<T> extends CodecPair<T, Encoder<T, ?>> {

    /**
     * Constructs a new {@link EncoderPair} for the given target type and encoder.
     *
     * @param target The type the encoder can encode.
     * @param codec  The encoder instance.
     */
    public EncoderPair(Class<T> target, Encoder<T, ?> codec) {
        super(target, codec);
    }

}
