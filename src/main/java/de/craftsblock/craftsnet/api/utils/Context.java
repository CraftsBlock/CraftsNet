package de.craftsblock.craftsnet.api.utils;

import de.craftsblock.craftsnet.utils.reflection.TypeUtils;
import org.jetbrains.annotations.NotNull;

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
 * @since 3.6.0
 */
public class Context extends ConcurrentHashMap<Class<?>, Object> {

    /**
     * Associates the specified value with the given class key in this context.
     * <p>
     * Before insertion, this method verifies that the provided value is an instance of
     * the specified key type using {@link TypeUtils#isInstance(Class, Object)}. If the
     * value is not compatible with the key type, an {@link IllegalArgumentException} is thrown.
     *
     * @param key   The class type to associate the value with
     * @param value The value to be stored
     * @return The previous value associated with the key, or {@code null} if none existed
     * @throws IllegalArgumentException If the value is not assignable to the given class key
     */
    @Override
    public Object put(@NotNull Class<?> key, @NotNull Object value) {
        if (!TypeUtils.isInstance(key, value))
            throw new IllegalArgumentException(
                    value.getClass().getName() + " is not a assignable to " + key.getName() + "!"
            );

        return super.put(key, value);
    }

    /**
     * Stores the given value in this context using its runtime class as the key.
     *
     * @param <T>   The type of the value being stored.
     * @param value The value to be stored.
     * @return The previous value associated with the value's class, or {@code null} if none existed.
     */
    public <T> Object put(@NotNull T value) {
        return super.put(value.getClass(), value);
    }

    /**
     * Retrieves the value associated with the given type key.
     *
     * @param <T>   The type of the value to retrieve.
     * @param key The class type key whose associated value is to be returned.
     * @return The value associated with the specified type, or {@code null} if none exists.
     */
    public <T> T getTyped(Class<T> key) {
        return getOrDefaultTyped(key, null);
    }

    /**
     * Retrieves the value associated with the given type key, returning a default value
     * if no mapping exists for the given type.
     *
     * @param <T>   The type of the value to retrieve.
     * @param key          The class type key whose associated value is to be returned.
     * @param defaultValue The default value to return if the key is not present.
     * @return The value associated with the specified type, or {@code defaultValue} if not found.
     * @throws ClassCastException If the stored value cannot be cast to the requested type.
     */
    public <T> T getOrDefaultTyped(Class<T> key, T defaultValue) {
        if (!containsKey(key))
            return defaultValue;

        Object value = super.get(key);
        Class<?> valueClass = value.getClass();

        if (!TypeUtils.isAssignable(key, valueClass))
            throw new ClassCastException("Could not cast class %s to %s".formatted(valueClass.getName(), key.getName()));

        return TypeUtils.cast(key, value);
    }

    /**
     * Searches this context for all values whose associated key type is assignable to the
     * specified target type.
     * <p>
     * This method collects all stored objects whose key type is compatible with the given
     * {@code type}. Compatibility is determined using {@link TypeUtils#isAssignable(Class, Class)}.
     * If no matching entries are found, an empty collection is returned.
     *
     * @param <T> The expected element type of the returned collection.
     * @param key The type used to filter matching entries.
     * @return A collection of values assignable to {@code type}, or an empty collection if none exist.
     */
    public <T> Collection<T> search(Class<T> key) {
        return search(key, Collections.emptyList());
    }

    /**
     * Searches this context for all values whose associated key type is assignable to the
     * specified target type, returning a fallback collection if no matches are found.
     * <p>
     * All stored objects whose key class is compatible with {@code type} are collected. If no
     * compatible entries exist, the method returns {@code orElse}, unless {@code orElse} itself
     * is empty, in that case an empty result list is returned instead.
     *
     * @param <T>          The expected element type of the returned collection.
     * @param key          The type used to filter matching entries.
     * @param defaultValue The fallback collection to return if no matching entries exist.
     * @return A collection of matching values, or {@code orElse} if no matches exist and
     * {@code orElse} is not empty.
     */
    public <T> Collection<T> search(Class<T> key, Collection<T> defaultValue) {
        List<T> matches = this.entrySet().stream()
                .filter((entry) -> TypeUtils.isAssignable(key, entry.getKey()))
                .map(entry -> TypeUtils.cast(key, entry.getValue()))
                .toList();

        if (!matches.isEmpty() || defaultValue.isEmpty()) return matches;
        return defaultValue;
    }

}
