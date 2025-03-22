package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftsnet.api.BaseExchange;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages session data, allowing the storage and retrieval of serialized objects
 * within a json file. The session data is stored in the specified location and
 * automatically serialized and deserialized when starting or stopping a session.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.3.0-SNAPSHOT
 */
public class Session extends ConcurrentHashMap<String, Object> implements AutoCloseable {

    private final SessionInfo sessionInfo;
    private final SessionStorage sessionStorage;

    private BaseExchange exchange;

    /**
     * Creates a new instance of {@code Session} without an active session.
     */
    public Session() {
        this.sessionInfo = new SessionInfo(this);
        this.sessionStorage = new SessionStorage(this);
    }

    /**
     * Sets the {@link BaseExchange} instance for this session storage.
     *
     * @param exchange The exchange used to load the session.
     * @throws IllegalStateException if the session file does not exist.
     */
    public void setExchange(@NotNull BaseExchange exchange) {
        if (this.exchange != null) {
            this.exchange = exchange;
            return;
        }

        this.exchange = exchange;
        this.sessionInfo.load();
    }

    /**
     * Retrieves the value associated with the specified key and attempts to cast it to the specified type.
     *
     * @param key  The key for the value to retrieve.
     * @param type The expected type of the value.
     * @param <T>  The type parameter of the value.
     * @return The value associated with the key, or {@code null} if not found or cannot be cast.
     */
    @SuppressWarnings("unchecked")
    public <T> T getAsType(@NotNull String key, @NotNull Class<T> type) {
        if (!containsKey(key)) return null;
        return (T) get(key);
    }

    /**
     * Retrieves the value associated with the specified key and attempts to cast it to the specified type.
     *
     * @param key      The key for the value to retrieve.
     * @param fallback The fallback value returned when the key does not exist.
     * @param type     The expected type of the value.
     * @param <T>      The type parameter of the value.
     * @return The value associated with the key, or {@code null} if not found or cannot be cast.
     */
    @SuppressWarnings("unchecked")
    public <T> T getAsType(@NotNull String key, T fallback, @NotNull Class<T> type) {
        if (!containsKey(key)) return fallback;
        return (T) get(key);
    }

    /**
     * Checks if the value associated with the specified key is of the specified type.
     *
     * @param key  The key for the value to check.
     * @param type The expected type of the value.
     * @param <T>  The type parameter of the value.
     * @return {@code true} if the value exists and matches the type, otherwise {@code false}.
     */
    public <T> boolean isType(@NotNull String key, @NotNull Class<T> type) {
        if (!containsKey(key)) return false;
        return type.isAssignableFrom(get(key).getClass());
    }

    /**
     * Retrieves the {@link BaseExchange} instance associated with this session.
     *
     * @return the associated {@code BaseExchange} instance, or {@code null} if none is set.
     */
    public BaseExchange getExchange() {
        return exchange;
    }

    /**
     * Stops the current session and deletes the associated session file.
     */
    public void stopSession() {
        this.sessionInfo.destroyPersistent();
    }

    /**
     * Starts a new session and generates a session identifier.
     *
     * @throws RuntimeException if the secure random passphrase generation fails.
     */
    public void startSession() {
        this.sessionInfo.makePersistent();
    }

    /**
     * Checks whether a session has been started.
     *
     * @return {@code true} if a session is active, otherwise {@code false}.
     */
    public boolean isSessionStarted() {
        return this.sessionInfo.isPersistent();
    }

    /**
     * Saves the session data to a persistent file. Automatically called when the session is closed.
     */
    @Override
    public void close() {
        this.sessionStorage.save();
    }

    /**
     * Retrieves the {@link SessionInfo} associated with this session.
     *
     * @return the {@code SessionInfo} instance managing session metadata.
     */
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Retrieves the {@link SessionStorage} associated with this session.
     *
     * @return the {@code SessionStorage} instance managing session persistence.
     */
    public SessionStorage getSessionStorage() {
        return sessionStorage;
    }

}
