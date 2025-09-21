package de.craftsblock.craftsnet.api.codec;

import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * A link between a {@link CodecPair} and the reflective {@link Method}
 * (encode or decode) of its {@link Codec}.
 *
 * @param codecPair The codec pair.
 * @param method    The reflective method reference (encode/decode).
 * @param <C>       The codec type.
 * @param <T>       The codec pair type.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Codec
 * @see CodecPair
 * @since 3.5.3
 */
public record CodecMethodLink<C extends Codec<?, ?>, T extends CodecPair<?, ?>>(@NotNull T codecPair, @NotNull Method method) {

    /**
     * @return the codec instance of this link.
     */
    @SuppressWarnings("unchecked")
    public @NotNull C codec() {
        return (C) codecPair.getCodec();
    }

    /**
     * Creates a new link between the given {@link CodecPair} and its
     * reflective encode/decode method.
     *
     * @param codecPair The codec pair to link.
     * @param <C>       The type of the codec.
     * @param <T>       The type of the codec pair.
     * @return A new {@link CodecMethodLink} instance.
     */
    public static <C extends Codec<?, ?>, T extends CodecPair<?, C>> @NotNull CodecMethodLink<C, T> create(@NotNull T codecPair) {
        var codec = codecPair.getCodec();

        Method method;
        if (codec instanceof Encoder<?, ?>)
            method = ReflectionUtils.findMethod(codec.getClass(), "encode", Object.class);
        else if (codec instanceof Decoder<?, ?>)
            method = ReflectionUtils.findMethod(codec.getClass(), "decode", Object.class);
        else throw new IllegalArgumentException("Unknown codec: %s".formatted(codec.getClass().getName()));

        if (method == null)
            throw new IllegalStateException("Could not create codec method ling to codec %s!".formatted(codec.getClass().getName()));

        return new CodecMethodLink<>(codecPair, method);
    }

}
