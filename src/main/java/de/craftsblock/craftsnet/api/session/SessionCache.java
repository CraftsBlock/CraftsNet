package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftscore.cache.Cache;

/**
 * A specialized cache for managing {@link Session} objects, allowing efficient storage
 * and retrieval of session instances using unique string keys. The cache has a fixed
 * capacity and employs an underlying caching mechanism to manage its entries.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see Session
 * @since 3.3.0-SNAPSHOT
 */
public class SessionCache extends Cache<String, Session> {

    /**
     * Constructs a new {@link SessionCache} with a specified maximum capacity.
     *
     * <p>The capacity defines the maximum number of sessions that can be held
     * in the cache. When the capacity is exceeded, the cache evicts the least
     * recently used entry.</p>
     *
     * @param capacity the maximum number of sessions the cache can hold.
     */
    public SessionCache(int capacity) {
        super(capacity);
    }

    /**
     * Retrieves a {@link Session} from the cache by its key. If the key does not exist
     * in the cache, a new instance of {@link Session} is created and returned.
     *
     * <p>This method ensures that a session is always returned, either from the cache
     * or as a new instance. The new session is not automatically added to the cache.</p>
     *
     * @param key The unique identifier for the session to retrieve.
     * @return The cached {@link Session} associated with the key, or a new {@link Session} instance
     * if the key is not present in the cache.
     */
    public Session getOrNew(String key) {
        return key != null && containsKey(key) ? get(key).getSessionInfo().secureSession() : new Session();
    }

}
