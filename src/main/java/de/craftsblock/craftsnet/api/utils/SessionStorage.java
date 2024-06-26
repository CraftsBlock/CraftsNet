package de.craftsblock.craftsnet.api.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A specialized ConcurrentHashMap implementation for storing session related data.
 * This class provides utility methods to retrieve stored values as specific types and to check
 * the type of stored values.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.6-SNAPSHOT
 */
public class SessionStorage extends ConcurrentHashMap<String, Object> {

    /**
     * Retrieves the value associated with the specified key and casts it to the specified type.
     * If the value is not present, returns null.
     *
     * @param key  the key whose associated value is to be returned
     * @param type the class of the type to cast the value to
     * @param <T>  the type of the value to return
     * @return the value associated with the specified key, cast to the specified type, or null if the key is not present
     * @throws ClassCastException if the value cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getAsType(@NotNull String key, @NotNull Class<T> type) {
        if (!containsKey(key)) return null;
        return (T) get(key);
    }

    /**
     * Checks if the value associated with the specified key is of the specified type.
     *
     * @param key  the key whose associated value is to be checked
     * @param type the class of the type to check the value against
     * @param <T>  the type to check the value against
     * @return true if the value associated with the specified key is of the specified type, false otherwise
     */
    public <T> boolean isType(@NotNull String key, @NotNull Class<T> type) {
        if (!containsKey(key)) return false;
        return type.isAssignableFrom(get(key).getClass());
    }

}
