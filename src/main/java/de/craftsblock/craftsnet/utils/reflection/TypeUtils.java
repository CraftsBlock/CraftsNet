package de.craftsblock.craftsnet.utils.reflection;

import org.jetbrains.annotations.Contract;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling Java primitive types and their wrapper classes.
 * Provides methods to check type assignability, convert between primitive and wrapper types,
 * and compare types for equivalence considering primitive-wrapper relationships.
 * <p>
 * This class is designed to assist reflection operations where understanding
 * the relationship between primitive types and their wrappers is essential.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.5.0
 */
public class TypeUtils {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            void.class, Void.class
    );

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE;

    static {
        Map<Class<?>, Class<?>> primitives = new HashMap<>(PRIMITIVE_TO_WRAPPER.size());
        for (Map.Entry<Class<?>, Class<?>> entry : PRIMITIVE_TO_WRAPPER.entrySet())
            primitives.put(entry.getValue(), entry.getKey());

        WRAPPER_TO_PRIMITIVE = primitives;
    }

    /**
     * Private constructor to prevent direct instantiation
     */
    private TypeUtils() {
    }

    /**
     * Determines if the {@code sourceType} can be assigned to the {@code targetType},
     * considering both primitive types and their corresponding wrapper classes.
     *
     * <p>
     * This method returns {@code true} if:
     * <ul>
     *     <li>{@code targetType} is assignable from {@code sourceType}</li>
     *     <li>{@code targetType} is a primitive and {@code sourceType} is its wrapper type</li>
     *     <li>{@code sourceType} is a primitive and {@code targetType} is its wrapper type</li>
     * </ul>
     * Otherwise, it returns {@code false}.
     *
     * @param targetType The target type to assign to; may be primitive or wrapper.
     * @param sourceType The source type to assign from; may be primitive or wrapper.
     * @return {@code true} if the {@code sourceType} is assignable to {@code targetType}, {@code false} otherwise.
     */
    @Contract(value = "null, null -> false", pure = true)
    public static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
        if (targetType == null || sourceType == null) return false;
        if (targetType.isAssignableFrom(sourceType)) return true;

        // Unpack array types
        if (targetType.isArray() && sourceType.isArray())
            return isAssignable(targetType.componentType(), sourceType.componentType());

        if (isPrimitive(targetType)) {
            var wrapper = toWrapper(targetType);
            return wrapper.equals(sourceType);
        }

        if (isPrimitive(sourceType)) {
            var wrapper = toWrapper(sourceType);
            return targetType.isAssignableFrom(wrapper);
        }

        return false;
    }

    /**
     * Converts the given {@code type} to its corresponding wrapper class if it is a primitive type.
     * If the given type is not primitive or is {@code null}, it is returned unchanged.
     *
     * @param type The type to convert; may be primitive or wrapper.
     * @return The wrapper class if {@code type} is primitive; otherwise, returns {@code type} unchanged.
     */
    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static Class<?> toWrapper(Class<?> type) {
        return type != null && type.isPrimitive()
                ? PRIMITIVE_TO_WRAPPER.get(type)
                : type;
    }

    /**
     * Converts the given {@code type} to its corresponding primitive class if it is a wrapper type.
     * If the given type is not a wrapper or is {@code null}, it is returned unchanged.
     *
     * @param type The type to convert; may be wrapper or primitive.
     * @return The primitive class if {@code type} is a wrapper; otherwise, returns {@code type} unchanged.
     */
    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static Class<?> toPrimitive(Class<?> type) {
        return WRAPPER_TO_PRIMITIVE.getOrDefault(type, type);
    }

    /**
     * Checks if the given {@code type} represents a primitive type.
     *
     * @param type The type to check.
     * @return {@code true} if {@code type} is a primitive type, {@code false} otherwise.
     */
    @Contract(value = "null -> false", pure = true)
    public static boolean isPrimitive(Class<?> type) {
        return type != null && type.isPrimitive();
    }

    /**
     * Checks if the given {@code type} represents a wrapper class of a primitive type.
     *
     * @param type The type to check.
     * @return {@code true} if {@code type} is a wrapper class for a primitive, {@code false} otherwise.
     */
    @Contract(value = "null -> false", pure = true)
    public static boolean isWrapper(Class<?> type) {
        return WRAPPER_TO_PRIMITIVE.containsKey(type);
    }

    /**
     * Determines if two types {@code a} and {@code b} are equivalent, considering primitive-wrapper equivalence.
     *
     * <p>For example, {@code int.class} and {@code Integer.class} are considered equivalent.</p>
     *
     * @param a The first type.
     * @param b The second type.
     * @return {@code true} if the two types are equivalent or both {@code null}, {@code false} otherwise.
     */
    public static boolean equals(Class<?> a, Class<?> b) {
        if (a == b) return true;
        if (a.isArray() && b.isArray()) return equals(a.componentType(), b.componentType());
        return toWrapper(a).equals(toWrapper(b));
    }

    /**
     * Converts the given {@link Type} into its corresponding {@link Class} representation, if possible.
     * <p>
     * This method supports the following cases:
     * <ul>
     *   <li>{@link Class}: returned as-is.</li>
     *   <li>{@link ParameterizedType}: returns its raw type as a {@link Class}.</li>
     *   <li>{@link GenericArrayType}: constructs an array class of the resolved component type.</li>
     *   <li>{@link TypeVariable}: resolves to the first bound or defaults to {@link Object}.</li>
     *   <li>{@link WildcardType}: resolves to the first upper bound or defaults to {@link Object}.</li>
     * </ul>
     *
     * @param type The {@link Type} to convert; may represent classes, generics, arrays, or wildcards.
     * @return The corresponding {@link Class} object.
     * @throws IllegalArgumentException if the given type cannot be converted to a {@link Class}.
     * @since 3.5.3
     */
    public static Class<?> convertTypeToClass(Type type) {
        if (type instanceof Class<?>) return (Class<?>) type;

        if (type instanceof ParameterizedType parameterizedType)
            return (Class<?>) parameterizedType.getRawType();

        if (type instanceof GenericArrayType genericArrayType) {
            Class<?> comp = convertTypeToClass(genericArrayType.getGenericComponentType());
            return Array.newInstance(comp, 0).getClass();
        }

        Type[] bounds;
        if (type instanceof TypeVariable<?> typeVariable)
            bounds = typeVariable.getBounds();
        else if (type instanceof WildcardType wildcardType)
            bounds = wildcardType.getUpperBounds();
        else bounds = null;

        if (bounds != null)
            return bounds.length > 0 ? convertTypeToClass(bounds[0]) : Object.class;

        throw new IllegalArgumentException("Unknown type: " + type);
    }

}
