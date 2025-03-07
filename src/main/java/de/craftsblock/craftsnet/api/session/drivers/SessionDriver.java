package de.craftsblock.craftsnet.api.session.drivers;

import de.craftsblock.craftsnet.api.session.Session;

import java.io.IOException;

/**
 * Interface representing a driver responsible for session persistence operations.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Session
 * @since 3.3.5-SNAPSHOT
 */
public interface SessionDriver {

    /**
     * Loads session data from the underlying storage into the provided session instance.
     *
     * @param session   The {@link Session} instance to be populated with data.
     * @param sessionID The unique identifier of the session.
     * @throws IOException If an error occurs while loading the session data.
     */
    void load(Session session, String sessionID) throws IOException;

    /**
     * Persists the provided session data to the underlying storage.
     *
     * @param session   The session instance containing data to be saved.
     * @param sessionID The unique identifier of the session.
     * @throws IOException If an error occurs while saving the session data.
     */
    void save(Session session, String sessionID) throws IOException;

    /**
     * Destroys or removes the session data associated with the given session identifier
     * from the underlying storage.
     *
     * @param session   The session instance to be destroyed.
     * @param sessionID The unique identifier of the session.
     * @throws IOException If an error occurs while destroying the session data.
     */
    void destroy(Session session, String sessionID) throws IOException;

}
