package de.craftsblock.craftsnet.api.codec.registry;

import de.craftsblock.craftsnet.api.codec.Codec;
import de.craftsblock.craftsnet.api.codec.CodecMethodLink;
import de.craftsblock.craftsnet.api.codec.CodecPair;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A base registry class for managing codecs that handle conversion between types.
 * <p>
 * This abstract class handles registration, lookup, and unregistration of codecs,
 * where each codec is associated with a target type.
 * <p>
 * The registry supports generic resolution of codec types through reflection, and allows
 * type-safe handling of codec pairs using a specialized {@link CodecPair} subclass.
 *
 * @param <C> the codec type being registered
 * @param <P> the pair type that wraps the codec and its target class
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.5.0
 */
public sealed abstract class TypeCodecRegistry<C extends Codec<?, ?>, P extends CodecPair<?, ?>>
        permits TypeDecoderRegistry, TypeEncoderRegistry {

    final ConcurrentHashMap<Class<?>, CodecMethodLink<C, P>> codecMethodLinks = new ConcurrentHashMap<>();
    final Map<Class<?>, CodecMethodLink<C, P>> unmodifiableCodecMethodLinksView = Collections.unmodifiableMap(codecMethodLinks);
    final Class<? extends CodecPair<?, C>> codecPairTyp;

    /**
     * Constructs a new {@link TypeCodecRegistry} with a given codec pair type.
     *
     * @param codecPairType The class type of the codec pair used in this registry.
     */
    @SuppressWarnings("unchecked")
    TypeCodecRegistry(Class<? extends P> codecPairType) {
        this.codecPairTyp = (Class<? extends CodecPair<?, C>>) codecPairType;
    }

    /**
     * Retrieves the constructor used to instantiate new codec pairs.
     *
     * @return The constructor of the codec pair type.
     */
    abstract @NotNull Constructor<? extends CodecPair<?, C>> getPairConstructor();

    /**
     * Registers a new codec into the registry.
     *
     * @param codec The codec instance to register.
     * @return The previously registered codec for the same type, or {@code null} if none existed.
     */
    @SuppressWarnings("unchecked")
    public CodecMethodLink<C, P> register(@NotNull C codec) {
        try {
            var type = this.retrieveCodecType(codec.getClass());

            var pair = getPairConstructor().newInstance(type, codec);
            return codecMethodLinks.put(type, (CodecMethodLink<C, P>) CodecMethodLink.create(pair));
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Could not generate %s!".formatted(this.codecPairTyp.getSimpleName()), e);
        }
    }

    /**
     * Unregisters the given codec from the registry.
     *
     * @param codec The codec to remove.
     */
    public void unregister(@NotNull C codec) {
        var type = this.retrieveCodecType(codec.getClass());
        this.codecMethodLinks.remove(type);
    }

    /**
     * Checks if the given codec is already registered.
     *
     * @param codec The codec to check.
     * @return {@code true} if registered, otherwise {@code false}.
     */
    public boolean isRegistered(@NotNull C codec) {
        var type = this.retrieveCodecType(codec.getClass());
        return this.codecMethodLinks.containsKey(type);
    }

    /**
     * Retrieves the codec associated with the given class type.
     *
     * @param type The target class.
     * @return The associated codec, or {@code null} if none found.
     */
    public @Nullable C getCodec(@Nullable Class<?> type) {
        return getLinkedCodecMethod(type).codec();
    }

    /**
     * Attempts to resolve a {@link CodecMethodLink} for the given type.
     * If no match is found, {@code null} is returned.
     *
     * @param type the class type to resolve a codec link for, may be {@code null}
     * @return the matching {@link CodecMethodLink}, or {@code null} if none is found
     * @since 3.5.3
     */
    public CodecMethodLink<C, P> getLinkedCodecMethod(@Nullable Class<?> type) {
        if (type == null) return null;

        if (codecMethodLinks.containsKey(type))
            return codecMethodLinks.get(type);

        // Handle superclass
        Class<?> superclass = type.getSuperclass();
        CodecMethodLink<C, P> superclassCodecLink = getLinkedCodecMethod(superclass);
        if (superclassCodecLink != null) return superclassCodecLink;

        // Handle interfaces
        for (Class<?> iface : type.getInterfaces()) {
            CodecMethodLink<C, P> codecLink = getLinkedCodecMethod(iface);
            if (codecLink == null) continue;
            return codecLink;
        }

        // Return null, as no linked codec could be found for the type
        return null;
    }

    /**
     * Checks whether a codec exists for the given class type.
     *
     * @param type The target class.
     * @return {@code true} if a codec exists, otherwise {@code false}.
     */
    public boolean hasCodec(@Nullable Class<?> type) {
        if (type == null) return false;
        if (codecMethodLinks.containsKey(type)) return true;

        // Handle superclass
        Class<?> superclass = type.getSuperclass();
        if (hasCodec(superclass)) return true;

        // Handle interfaces
        for (Class<?> iface : type.getInterfaces())
            if (hasCodec(iface)) return true;

        // Return false, as no codec could be found for the type
        return false;
    }

    /**
     * Retrieves all {@link Codec codecs} currently registered in this registry.
     *
     * @return An unmodifiable {@link Collection} of all {@link Codec codecs}.
     */
    public @NotNull @Unmodifiable Collection<C> getCodecs() {
        return codecMethodLinks.values().stream().map(CodecMethodLink::codec).toList();
    }

    /**
     * Retrieves all {@link CodecPair codec pairs} currently registered in this registry.
     *
     * @return An unmodifiable {@link Collection} of all {@link CodecPair codec pairs}.
     */
    public @NotNull @Unmodifiable Collection<CodecMethodLink<C, P>> getCodecPairs() {
        return unmodifiableCodecMethodLinksView.values();
    }

    /**
     * Retrieves all known types currently registered in the codec registry.
     * <p>
     * This collection represents all classes for which a codec has been registered,
     * providing a snapshot of all supported target types.
     *
     * @return An unmodifiable {@link Collection} of registered {@link Class} types.
     */
    public @NotNull @Unmodifiable Collection<Class<?>> getAllKnownTypes() {
        return unmodifiableCodecMethodLinksView.keySet();
    }

    /**
     * Resolves the target type from a codec class using generic type extraction.
     *
     * @param type The codec implementation class.
     * @return The class representing the target type the codec supports.
     */
    private Class<?> retrieveCodecType(@NotNull Class<?> type) {
        return ReflectionUtils.extractGenericInterface(type, 0);
    }

}
