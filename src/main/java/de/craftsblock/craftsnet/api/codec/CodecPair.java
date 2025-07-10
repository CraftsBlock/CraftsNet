package de.craftsblock.craftsnet.api.codec;

import de.craftsblock.craftsnet.utils.reflection.TypeUtils;
import org.jetbrains.annotations.ApiStatus;

/**
 * A sealed base class representing a binding between a target type and a corresponding codec.
 * <p>
 * This class is extended by {@link DecoderPair} and {@link EncoderPair} to handle specific
 * codec types. It provides utility methods to validate and adapt target types.
 *
 * @param <T> The target type the codec supports.
 * @param <C> The codec type, which must implement {@link Codec}.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see DecoderPair
 * @see EncoderPair
 * @since 3.5.0
 */
@ApiStatus.Experimental
public sealed abstract class CodecPair<T, C extends Codec<?, ?>>
        permits DecoderPair, EncoderPair {

    private final Class<T> target;
    private final C codec;

    /**
     * Constructs a new {@link CodecPair} with the given target type and codec.
     *
     * @param target The target type the codec supports.
     * @param codec  The codec instance.
     */
    public CodecPair(Class<T> target, C codec) {
        this.target = target;
        this.codec = codec;
    }

    /**
     * Determines whether the given type can be handled by this codec.
     *
     * @param type The type to check.
     * @return {@code true} if the codec supports or is assignable from the given type, otherwise {@code false}.
     */
    public boolean canCodecType(Class<?> type) {
        if (this.target.equals(type)) return true;
        return TypeUtils.isAssignable(this.target, type);
    }

    /**
     * Casts the given type to a subclass of the target type.
     *
     * @param type The type to cast.
     * @return The casted class.
     * @throws ClassCastException if {@code type} is not a subclass of the target.
     */
    public Class<? extends T> asCodecSubtype(Class<?> type) {
        return type.asSubclass(this.target);
    }

    /**
     * Returns the target type of this codec pair.
     *
     * @return The target class.
     */
    public Class<T> getTarget() {
        return this.target;
    }

    /**
     * Returns the codec associated with this pair.
     *
     * @return The codec instance.
     */
    public C getCodec() {
        return this.codec;
    }

}
