package de.craftsblock.craftsnet.api.codec.registry;

import de.craftsblock.craftsnet.api.codec.*;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

/**
 * A specialized implementation of {@link TypeCodecRegistry} for managing {@link Encoder} instances.
 * <p>
 * This registry maps encoders to the Java types they support, allowing for efficient lookup,
 * registration, and removal of type-bound {@link EncoderPair}s.
 * <p>
 * It uses generic type reflection to associate each {@link Encoder} with its target class,
 * enabling dynamic codec resolution based on type hierarchies.
 *
 * @param <E> the type of encoder being managed
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Encoder
 * @see EncoderPair
 * @see TypeCodecRegistry
 * @since 3.5.0
 */
public final class TypeEncoderRegistry<E extends Encoder<?, ?>> extends TypeCodecRegistry<E, EncoderPair<?>> {

    /**
     * Constructs a new {@link TypeEncoderRegistry} instance.
     * <p>
     * The constructor uses type reflection to resolve the generic encoder pair type
     * associated with this registry.
     */
    public TypeEncoderRegistry() {
        super(ReflectionUtils.extractGeneric(TypeEncoderRegistry.class, 1));
    }

    /**
     * Provides the constructor used to instantiate {@link EncoderPair}s.
     * <p>
     * This constructor is necessary for creating codec pairs dynamically
     * when registering new encoders.
     *
     * @return A constructor that accepts the target class and encoder instance.
     */
    @Override
    @NotNull
    Constructor<? extends CodecPair<?, E>> getPairConstructor() {
        return ReflectionUtils.findConstructor(this.codecPairTyp, Class.class, Encoder.class);
    }

}
