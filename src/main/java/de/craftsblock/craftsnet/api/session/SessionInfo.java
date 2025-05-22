package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.cookies.Cookie;
import de.craftsblock.craftsnet.api.http.cookies.SameSite;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.PassphraseUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Manages session metadata and handles the persistence and lifecycle of sessions.
 * The {@link SessionInfo} class works in conjunction with {@link Session} to manage
 * session identifiers and integrate sessions with HTTP exchanges and caches.
 *
 * <p>It provides methods to load sessions, make them persistent, and destroy them.
 * Compatibility with HTTP exchanges is ensured through validation methods.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 3.1.0
 * @see Session
 * @see BaseExchange
 * @since 3.0.6-SNAPSHOT
 */
public class SessionInfo {

    /**
     * The name of the cookie used to store session identifiers.
     */
    public static final String SID_COOKIE_NAME = "CNET_SID";

    /**
     * The reference {@link Cookie}
     */
    public static final Cookie REFERENCE_COOKIE = new Cookie(SID_COOKIE_NAME, null).setHttpOnly(true)
            .setPath("/").setSameSite(SameSite.LAX);

    /**
     * Setting the reference {@link Cookie cookie} whose parameters are used when
     * session cookies are created or deleted.
     *
     * @param referenceCookie The reference instance {@link Cookie cookie}.
     * @since 3.3.6-SNAPSHOT
     */
    public static void setReferenceCookie(Cookie referenceCookie) {
        REFERENCE_COOKIE.override(referenceCookie);
    }

    private final Session session;

    private CraftsNet craftsNet;
    private Logger logger;
    private String sessionID = null;
    private boolean persistent = false;

    /**
     * Creates a new {@link SessionInfo} instance associated with the specified session.
     *
     * @param session the session associated with this metadata.
     */
    public SessionInfo(Session session) {
        this.session = session;
    }

    /**
     * Loads session metadata from the exchange and integrates it into the session system.
     * The session is marked as persistent if a session ID is found.
     */
    protected void load() {
        BaseExchange exchange = session.getExchange();

        if (exchange instanceof Exchange http) {
            this.craftsNet = http.response().getCraftsNet();
            this.logger = http.request().getCraftsNet().logger();
            this.sessionID = extractSession(http.request());
        } else if (exchange instanceof SocketExchange) {
            // Currently not implemented / supported
            return;
        }

        if (this.sessionID == null) return;

        // Must be true because SessionStorage#exists relies on #persistent
        this.persistent = true;
        if (!this.session.getSessionStorage().exists()) {
            this.persistent = false;
            return;
        }

        if (this.craftsNet != null)
            this.craftsNet.sessionCache().put(this.sessionID, this.session);

        this.session.getSessionStorage().load();
    }

    /**
     * Makes the session persistent by generating a unique session ID and updating
     * the HTTP response with a session cookie. The session is added to the session cache.
     *
     * @throws RuntimeException if a secure random passphrase cannot be generated.
     */
    protected void makePersistent() {
        compatibleOrThrow();
        if (isPersistent()) return;

        this.sessionID = PassphraseUtils.generateSecure(20, false);
        this.persistent = true;

        if (this.session.getExchange() instanceof Exchange http)
            http.response().setCookie(SID_COOKIE_NAME)
                    .override(REFERENCE_COOKIE).setValue(this.sessionID);

        if (craftsNet != null)
            craftsNet.sessionCache().put(this.sessionID, this.session);
    }

    /**
     * Destroys the persistent session by removing it from the cache, deleting the session file,
     * and clearing the session cookie in the HTTP response.
     */
    protected void destroyPersistent() {
        compatibleOrThrow();
        if (!isPersistent()) return;

        if (craftsNet != null) craftsNet.sessionCache().remove(this.sessionID);
        this.session.getSessionStorage().destroy();

        if (this.session.getExchange() instanceof Exchange http)
            http.response().deleteCookie(SID_COOKIE_NAME)
                    .override(REFERENCE_COOKIE);

        this.sessionID = null;
        this.persistent = false;
    }

    /**
     * Ensures the current exchange is compatible with the session system.
     * Throws an exception if the compatibility check fails.
     *
     * @throws IllegalStateException if the current exchange is not compatible.
     */
    private void compatibleOrThrow() {
        if (this.session.getExchange() instanceof Exchange http) {
            if (http.response().headersSent())
                throw new IllegalStateException("The response headers have already been sent!");
            return;
        }

        throw new IllegalStateException("The current exchange is not compatible with the session system!");
    }

    /**
     * Retrieves the {@link CraftsNet} instance associated with this session, if available.
     *
     * @return the {@link CraftsNet} instance or {@code null} if not available.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

    /**
     * Retrieves the {@link Logger} instance associated with this session, if available.
     *
     * @return the {@link Logger} instance or {@code null} if not available.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Retrieves the session ID associated with this session, if available.
     *
     * @return the session ID or {@code null} if the session is not persistent.
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * Checks whether the session is persistent.
     *
     * @return {@code true} if the session is persistent, otherwise {@code false}.
     */
    public boolean isPersistent() {
        return persistent && this.sessionID != null;
    }

    /**
     * Retrieves the {@link Session} instance associated with this metadata.
     *
     * @return the associated {@code Session}.
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * Extracts the session ID from the request cookies.
     *
     * @param request the HTTP request to extract the session ID from.
     * @return the session ID or {@code null} if no session ID is present.
     */
    public static @Nullable String extractSession(Request request) {
        if (!request.hasCookie(SID_COOKIE_NAME)) return null;
        return Objects.requireNonNull(request.retrieveCookie(SID_COOKIE_NAME)).getValue();
    }

}
