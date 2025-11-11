package de.craftsblock.craftsnet.api.ssl;

import de.craftsblock.craftsnet.utils.reflection.TypeUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A specialized {@link ConcurrentHashMap} implementation that serves as a type-safe context
 * for storing and retrieving objects associated with their respective {@link Class} types.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.5.6
 */
public class Context extends ConcurrentHashMap<Class<?>, Object> {

    /**
     * Retrieves the value associated with the given key.
     * <p>
     * This method is deprecated in favor of {@link #get(Class)} and {@link #get(Class, Object)},
     * which provide type-safe access to the stored objects.
     *
     * @param key The key whose associated value is to be returned
     * @return The value associated with the specified key if the key is a {@link Class},
     * or {@code null} otherwise
     * @deprecated Use {@link #get(Class)} instead for type-safe retrieval
     */
    @Override
    @Deprecated(since = "3.5.6")
    public Object get(Object key) {
        if (key instanceof Class<?> type)
            return get(type);
        return null;
    }

    /**
     * Retrieves the value associated with the given type key.
     *
     * @param <T>  The expected type of the returned object
     * @param type The class type key whose associated value is to be returned
     * @return The value associated with the specified type, or {@code null} if none exists
     */
    public <T> T get(Class<T> type) {
        return get(type, null);
    }

    /**
     * Retrieves the value associated with the given type key, returning a default value
     * if no mapping exists for the given type.
     *
     * @param <T>    The expected type of the returned object
     * @param type   The class type key whose associated value is to be returned
     * @param orElse The default value to return if the key is not present
     * @return The value associated with the specified type, or {@code orElse} if not found
     * @throws ClassCastException If the stored value cannot be cast to the requested type
     */
    public <T> T get(Class<T> type, T orElse) {
        if (type == null || !containsKey(type))
            return orElse;

        Object value = super.get(type);
        Class<?> valueClass = value.getClass();

        if (!TypeUtils.isAssignable(type, valueClass))
            throw new ClassCastException("Could not cast class %s to %s".formatted(valueClass.getName(), type.getName()));

        return type.cast(value);
    }

}
