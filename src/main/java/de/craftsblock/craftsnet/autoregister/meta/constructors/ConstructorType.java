package de.craftsblock.craftsnet.autoregister.meta.constructors;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Enum representing different types of constructors used in automatic instance creation
 * or dependency injection mechanisms.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see FallbackConstructor
 * @see IgnoreConstructor
 * @see PreferConstructor
 * @since 3.3.5-SNAPSHOT
 */
public enum ConstructorType {

    /**
     * A constructor marked with {@link IgnoreConstructor}, meaning it should be ignored.
     */
    IGNORED(IgnoreConstructor.class),

    /**
     * A constructor marked with {@link PreferConstructor}, indicating that it should be preferred.
     */
    PREFERRED(PreferConstructor.class),

    /**
     * A normal constructor without any special annotation.
     */
    NORMAL(null),

    /**
     * A constructor marked with {@link FallbackConstructor}, which serves as a last resort.
     */
    FALLBACK(FallbackConstructor.class),
    ;

    /**
     * Array containing all constructor types that require annotations.
     */
    private static final ConstructorType[] specialTypes;

    static {
        List<ConstructorType> types = new ArrayList<>();
        for (ConstructorType type : values()) {
            if (!type.needsAnnotation()) continue;
            types.add(type);
        }

        specialTypes = types.toArray(ConstructorType[]::new);
    }

    /**
     * The annotation associated with this constructor type, if applicable.
     */
    private final @Nullable Class<? extends Annotation> annotation;

    /**
     * Constructs a {@link ConstructorType} with an optional associated annotation.
     *
     * @param annotation The annotation class associated with this constructor type, or {@code null} if none.
     */
    ConstructorType(@Nullable Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    /**
     * Gets the annotation associated with this constructor type.
     *
     * @return The annotation class, or {@code null} if none is associated.
     */
    public @Nullable Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    /**
     * Determines whether this constructor type requires an annotation.
     *
     * @return {@code true} if this type requires an annotation, {@code false} otherwise.
     */
    public boolean needsAnnotation() {
        return this.annotation != null;
    }

    /**
     * Estimates the constructor type based on the annotations present on the given constructor.
     *
     * @param constructor The constructor to evaluate.
     * @return The corresponding {@link  ConstructorType}, defaulting to {@link #NORMAL} if no special annotations are found.
     */
    public static ConstructorType estimateType(Constructor<?> constructor) {
        for (ConstructorType type : specialTypes) {
            if (type.getAnnotation() == null) continue;
            if (constructor.getAnnotation(type.getAnnotation()) == null) continue;
            return type;
        }

        return NORMAL;
    }

}
