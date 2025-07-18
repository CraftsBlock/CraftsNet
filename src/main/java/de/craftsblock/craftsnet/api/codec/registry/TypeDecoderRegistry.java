package de.craftsblock.craftsnet.api.codec.registry;

import de.craftsblock.craftsnet.api.codec.CodecPair;
import de.craftsblock.craftsnet.api.codec.Decoder;
import de.craftsblock.craftsnet.api.codec.DecoderPair;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

/**
 * A concrete implementation of {@link TypeCodecRegistry} for managing {@link Decoder} instances.
 * <p>
 * This registry binds decoders to their respective target types using {@link DecoderPair}s and
 * enables type-safe registration, lookup, and unregistration.
 *
 * @param <D> the type of decoder being managed
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Decoder
 * @see DecoderPair
 * @see TypeCodecRegistry
 * @since 3.5.0
 */
public final class TypeDecoderRegistry<D extends Decoder<?, ?>> extends TypeCodecRegistry<D, DecoderPair<?>> {

    /**
     * Constructs a new {@link TypeDecoderRegistry} instance.
     * <p>
     * The constructor uses reflection to determine the generic type of the decoder pair class
     * used in this registry.
     */
    public TypeDecoderRegistry() {
        super(ReflectionUtils.extractGeneric(TypeDecoderRegistry.class, 1));
    }

    /**
     * Provides the constructor used to instantiate {@link DecoderPair}s.
     * <p>
     * This constructor is required to dynamically create new codec pairs at runtime
     * when registering decoders.
     *
     * @return A constructor that accepts the target class and decoder instance.
     */
    @Override
    @NotNull
    Constructor<? extends CodecPair<?, D>> getPairConstructor() {
        return ReflectionUtils.findConstructor(this.codecPairTyp, Class.class, Decoder.class);
    }

}
