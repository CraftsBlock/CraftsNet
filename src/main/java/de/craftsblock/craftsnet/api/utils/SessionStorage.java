package de.craftsblock.craftsnet.api.utils;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.api.BaseExchange;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.cookies.SameSite;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages session data, allowing the storage and retrieval of serialized objects
 * within a json file. The session data is stored in the specified location and
 * automatically serialized and deserialized when starting or stopping a session.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.0
 * @since 3.0.6-SNAPSHOT
 */
public class SessionStorage extends ConcurrentHashMap<String, Object> implements AutoCloseable {

    private static final String STORAGE_LOCATION = "./sessions";
    private static final String SID_COOKIE_NAME = "CNET_SID";

    private Logger logger;
    private BaseExchange exchange;
    private String session;

    /**
     * Creates a new instance of {@code SessionStorage} without an active session.
     */
    public SessionStorage() {
    }

    /**
     * Sets the {@link BaseExchange} instance for this session storage.
     *
     * @param exchange The exchange used to load the session.
     * @throws IllegalStateException if the session file does not exist.
     */
    public void setExchange(@NotNull BaseExchange exchange) {
        if (this.exchange != null) return;
        this.exchange = exchange;

        if (exchange instanceof Exchange http) {
            logger = http.request().getCraftsNet().logger();
            if (!http.request().hasCookie(SID_COOKIE_NAME)) return;
            session = Objects.requireNonNull(http.request().retrieveCookie(SID_COOKIE_NAME)).getValue();
        } else if (exchange instanceof SocketExchange) {
            // Currently not implemented / supported
            return;
        }

        File data = new File(STORAGE_LOCATION, session + ".json");
        try {
            if (data.createNewFile()) return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Json json = JsonParser.parse(data);
        for (String key : json.keySet()) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(Utils.convert(json.getByteList(key).toArray(Byte[]::new)));
                 ObjectInputStream reader = new ObjectInputStream(in)) {
                put(key, reader.readObject());
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Stops the current session and deletes the associated session file.
     *
     * @return {@code true} if the session was successfully stopped, otherwise {@code false}.
     */
    public boolean stopSession() {
        compatibleOrThrow();
        if (!isSessionStarted()) return false;

        File data = new File(STORAGE_LOCATION, session + ".json");
        if (data.exists()) data.delete();

        if (this.exchange instanceof Exchange http)
            http.response().deleteCookie(SID_COOKIE_NAME).setHttpOnly(true)
                    .setPath("/").setSameSite(SameSite.LAX);

        session = null;
        return true;
    }

    /**
     * Starts a new session and generates a session identifier.
     *
     * @return The generated session identifier.
     * @throws RuntimeException if the secure random passphrase generation fails.
     */
    public String startSession() {
        compatibleOrThrow();
        if (isSessionStarted()) return session;

        try {
            session = Utils.secureRandomPassphrase(20, false);

            if (this.exchange instanceof Exchange http)
                http.response().setCookie(SID_COOKIE_NAME, session).setHttpOnly(true)
                        .setPath("/").setSameSite(SameSite.LAX);

            return session;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether a session has been started.
     *
     * @return {@code true} if a session is active, otherwise {@code false}.
     */
    public boolean isSessionStarted() {
        return session != null;
    }

    private void compatibleOrThrow() {
        if (this.exchange instanceof Exchange http) {
            if (http.response().headersSent())
                throw new IllegalStateException("The response headers have already been sent!");
            return;
        }

        throw new IllegalStateException("");
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
     * Serializes and stores the session data into a json file upon closing the session.
     * If no session is active, this method does nothing.
     *
     * @throws Exception if an error occurs while closing or writing to the file.
     */
    @Override
    public void close() throws Exception {
        try {
            if (!isSessionStarted()) return;

            File data = new File(STORAGE_LOCATION, session + ".json");
            Json json = Json.empty();

            for (Map.Entry<String, Object> entry : entrySet())
                try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                     ObjectOutputStream writer = new ObjectOutputStream(out)) {
                    writer.writeObject(entry.getValue());
                    json.set(entry.getKey(), Utils.convert(out.toByteArray()));
                } catch (NotSerializableException e) {
                    logger.error(e, "Skipping key " + entry.getKey());
                }

            json.save(data);
        } finally {
            clear();
        }
    }

}
