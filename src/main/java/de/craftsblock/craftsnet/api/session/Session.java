package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Manages session data, allowing the storage and retrieval of serialized objects.
 * The session data is stored in the specified location and
 * automatically serialized and deserialized when starting or stopping a session.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @since 3.3.0-SNAPSHOT
 */
public class Session extends ConcurrentHashMap<String, Object> implements AutoCloseable {

    private final SessionInfo sessionInfo;
    private final SessionStorage sessionStorage;

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    protected BaseExchange exchange;

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
        boolean loaded = this.exchange != null;
        this.exchange = exchange;

        if (loaded) {
            return;
        }

        this.sessionInfo.load();
        this.markClean();
    }

    /**
     * {@inheritDoc}
     *
     * @param key   {@inheritDoc}
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object put(@NotNull String key, @NotNull Object value) {
        Object previous = super.put(key, value);
        markDirty();
        return previous;
    }

    /**
     * {@inheritDoc}
     *
     * @param map {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends String, ?> map) {
        if (map.isEmpty()) {
            return;
        }

        super.putAll(map);
        markDirty();
    }

    /**
     * {@inheritDoc}
     *
     * @param key {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object remove(@NotNull Object key) {
        Object previous = super.remove(key);
        if (previous != null) {
            markDirty();
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     *
     * @param key   {@inheritDoc}
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean remove(Object key, Object value) {
        boolean removed = super.remove(key, value);
        if (removed) {
            markDirty();
        }

        return removed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        if (isEmpty()) {
            return;
        }

        super.clear();
        markDirty();
    }

    /**
     * {@inheritDoc}
     *
     * @param key   {@inheritDoc}
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object putIfAbsent(@NotNull String key, Object value) {
        Object previous = super.putIfAbsent(key, value);
        if (previous == null) {
            markDirty();
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     *
     * @param key   {@inheritDoc}
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object replace(@NotNull String key, @NotNull Object value) {
        Object previous = super.replace(key, value);
        if (previous != null) {
            markDirty();
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     *
     * @param key      {@inheritDoc}
     * @param oldValue {@inheritDoc}
     * @param newValue {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean replace(@NotNull String key, @NotNull Object oldValue, @NotNull Object newValue) {
        boolean replaced = super.replace(key, oldValue, newValue);
        if (replaced) {
            markDirty();
        }

        return replaced;
    }

    /**
     * {@inheritDoc}
     *
     * @param function {@inheritDoc}
     */
    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        if (isEmpty()) {
            return;
        }

        super.replaceAll(function);
        markDirty();
    }

    /**
     * {@inheritDoc}
     *
     * @param key      {@inheritDoc}
     * @param function {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object computeIfAbsent(String key, @NotNull Function<? super String, ?> function) {
        Object previous = super.get(key);
        Object result = super.computeIfAbsent(key, function);
        if (previous == null && result != null) {
            markDirty();
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @param key      {@inheritDoc}
     * @param function {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object computeIfPresent(
            String key,
            @NotNull BiFunction<? super String, ? super Object, ?> function
    ) {
        Object previous = super.get(key);
        Object result = super.computeIfPresent(key, function);
        if (previous != null) {
            markDirty();
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @param key      {@inheritDoc}
     * @param function {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object compute(
            String key,
            @NotNull BiFunction<? super String, ? super Object, ?> function
    ) {
        Object previous = super.get(key);
        Object result = super.compute(key, function);
        if (previous != result) {
            markDirty();
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @param key      {@inheritDoc}
     * @param value    {@inheritDoc}
     * @param function {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Object merge(
            String key,
            @NotNull Object value,
            @NotNull BiFunction<? super Object, ? super Object, ?> function
    ) {
        Object result = super.merge(key, value, function);
        markDirty();
        return result;
    }

    /**
     * Retrieves the value associated with the specified key and attempts to cast it to the specified type.
     *
     * @param key  The key for the value to retrieve.
     * @param type The expected type of the value.
     * @param <T>  The type parameter of the value.
     * @return The value associated with the key, or {@code null} if not found or cannot be cast.
     * @since 3.7.0
     */
    public <T> T getTyped(@NotNull String key, @NotNull Class<T> type) {
        return this.getTyped(key, type, null);
    }

    /**
     * Retrieves the value associated with the specified key and attempts
     * to cast it to the specified type.
     *
     * @param key    The key for the value to retrieve.
     * @param orElse The fallback value returned when the key does not exist.
     * @param type   The expected type of the value.
     * @param <T>    The type parameter of the value.
     * @return The value associated with the key, or the value of {@code orElse} if
     * not found or cannot be cast.
     * @since 3.7.0
     */
    @SuppressWarnings("unchecked")
    public <T> T getTyped(@NotNull String key, @NotNull Class<T> type, T orElse) {
        if (!containsKey(key)) {
            return orElse;
        }

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
        if (!containsKey(key)) {
            return false;
        }

        return TypeUtils.isAssignable(type, get(key).getClass());
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
     * Checks whether the session data has been modified.
     *
     * @return {@code true} if the session data has been modified, otherwise {@code false}.
     */
    public boolean isDirty() {
        return dirty.get();
    }

    /**
     * Marks the session data as modified.
     */
    public void markDirty() {
        dirty.set(true);
    }

    /**
     * Marks the session data as clean.
     * Should be called after successfully saving or loading the session.
     */
    public void markClean() {
        dirty.set(false);
    }

    /**
     * Saves the session data to a persistent file. Automatically called when the session is closed.
     */
    @Override
    public void close() {
        if (!this.isDirty()) {
            return;
        }

        this.sessionStorage.save();
        this.markClean();
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
