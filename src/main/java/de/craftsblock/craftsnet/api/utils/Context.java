package de.craftsblock.craftsnet.api.utils;

import de.craftsblock.craftsnet.utils.reflection.TypeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
     * Checks whether the map contains a mapping for the specified key.
     * <p>
     * This deprecated override only supports keys of type {@link Class}. If the
     * provided key is not a class, the method returns {@code false}. Prefer using
     * {@link #containsKey(Class)} for type-safe lookups.
     *
     * @param key The key whose presence in this map is to be tested
     * @return {@code true} if the key is a {@link Class} and a corresponding mapping exists,
     * otherwise {@code false}
     * @deprecated since 3.5.6; use {@link #containsKey(Class)} instead
     */
    @Deprecated(since = "3.5.6")
    @Override
    public boolean containsKey(Object key) {
        if (key instanceof Class<?> type)
            return containsKey(type);
        return false;
    }

    /**
     * Checks whether the map contains a mapping for the specified class key.
     *
     * @param key The class key whose presence in this map is to be tested
     * @return {@code true} if a value is mapped to the given class key, otherwise {@code false}
     */
    public boolean containsKey(Class<?> key) {
        return super.containsKey(key);
    }

    /**
     * Retrieves the value associated with the given key.
     * <p>
     * This method is deprecated in favor of {@link #get(Class)} and {@link #getOrDefault(Class, Object)},
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
        return getOrDefault(type, null);
    }

    /**
     * Retrieves the value associated with the given key or returns the provided
     * default value if no mapping exists.
     * <p>
     * This deprecated version only supports keys of type {@link Class}. If the
     * supplied key is not a class, the method returns {@code null}. Prefer using
     * {@link #getOrDefault(Class, Object)} for type-safe lookups.
     *
     * @param key          The key whose associated value is to be returned
     * @param defaultValue The default value to return if no mapping exists
     * @return The associated value if present, {@code defaultValue} if not,
     * or {@code null} if the key is not a {@link Class}
     * @deprecated since 3.5.6; use {@link #getOrDefault(Class, Object)} instead
     */
    @Override
    @Deprecated(since = "3.5.6")
    public Object getOrDefault(Object key, Object defaultValue) {
        if (key instanceof Class<?> type)
            return containsKey(type) ? get(key) : defaultValue;
        return null;
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
    public <T> T getOrDefault(Class<T> type, T orElse) {
        if (type == null || !containsKey(type))
            return orElse;

        Object value = super.get(type);
        Class<?> valueClass = value.getClass();

        if (!TypeUtils.isAssignable(type, valueClass))
            throw new ClassCastException("Could not cast class %s to %s".formatted(valueClass.getName(), type.getName()));

        return type.cast(value);
    }

    /**
     * Searches this context for all values whose associated key type is assignable to the
     * specified target type.
     * <p>
     * This method collects all stored objects whose key type is compatible with the given
     * {@code type}. Compatibility is determined using {@link TypeUtils#isAssignable(Class, Class)}.
     * If no matching entries are found, an empty collection is returned.
     *
     * @param <T>  The expected element type of the returned collection
     * @param type The type used to filter matching entries
     * @return A collection of values assignable to {@code type}, or an empty collection if none exist
     */
    public <T> Collection<T> search(Class<T> type) {
        return search(type, Collections.emptyList());
    }

    /**
     * Searches this context for all values whose associated key type is assignable to the
     * specified target type, returning a fallback collection if no matches are found.
     * <p>
     * All stored objects whose key class is compatible with {@code type} are collected. If no
     * compatible entries exist, the method returns {@code orElse}, unless {@code orElse} itself
     * is empty, in that case an empty result list is returned instead.
     *
     * @param <T>    The expected element type of the returned collection
     * @param type   The type used to filter matching entries
     * @param orElse The fallback collection to return if no matching entries exist
     * @return A collection of matching values, or {@code orElse} if no matches exist and
     * {@code orElse} is not empty
     */
    public <T> Collection<T> search(Class<T> type, Collection<T> orElse) {
        List<T> matches = this.entrySet().stream()
                .filter((entry) -> TypeUtils.isAssignable(type, entry.getKey()))
                .map(entry -> TypeUtils.cast(type, entry.getValue()))
                .toList();

        if (!matches.isEmpty() || orElse.isEmpty()) return matches;
        return orElse;
    }

}
