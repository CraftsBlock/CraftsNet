package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftscore.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A specialized ConcurrentHashMap implementation for storing WebSocket related data.
 * This class provides utility methods to retrieve stored values as specific types and to check
 * the type of stored values.
 */
public class WebSocketStorage extends ConcurrentHashMap<String, Object> {

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
