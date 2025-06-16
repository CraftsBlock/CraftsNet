package de.craftsblock.craftsnet.api.http.body;

import de.craftsblock.craftsnet.api.http.body.parser.JsonBodyParser;
import de.craftsblock.craftsnet.api.http.body.parser.MultipartFormBodyParser;
import de.craftsblock.craftsnet.api.http.body.parser.StandardFormBodyParser;
import de.craftsblock.craftsnet.api.http.body.parser.*;
import de.craftsblock.craftsnet.api.http.body.parser.typed.JsonBodyParser;
import de.craftsblock.craftsnet.utils.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a registry for body parsers used in HTTP request processing.
 * It allows registering parsers for different types of request bodies,
 * and provides methods to retrieve and check the presence of a parser for a given body type.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.3
 * @see BodyParser
 * @since 3.0.4-SNAPSHOT
 */
public class BodyRegistry {

    private final ConcurrentHashMap<Class<? extends Body>, BodyParser<? extends Body>> bodyParser = new ConcurrentHashMap<>();

    /**
     * Constructs a new BodyRegistry and registers default body parsers for common body types.
     * This constructor initializes the registry with parsers for JSON, multipart form, and standard form bodies.
     */
    public BodyRegistry() {
        register(new JsonBodyParser());
        register(new MultipartFormBodyParser());
        register(new StandardFormBodyParser());
    }

    /**
     * Registers a body parser for a specific body type.
     *
     * @param bodyParser The body parser for the specified body type.
     * @param <T>        The type of body being parsed.
     */
    public <T extends Body> void register(@NotNull BodyParser<T> bodyParser) {
        if (isRegistered(bodyParser)) return;
        this.bodyParser.put(Objects.requireNonNull(ReflectionUtils.extractGeneric(bodyParser.getClass(), BodyParser.class, 0)), bodyParser);
    }

    /**
     * Checks if the given {@link BodyParser} is registered.
     * This class is a wrapper for {@link BodyRegistry#isRegistered(Class)}.
     *
     * @param loader The {@link BodyParser} to check.
     * @return {@code true} when the {@link BodyParser} was registered, {@code false} otherwise.
     * @since 3.2.1-SNAPSHOT
     */
    @SuppressWarnings("unchecked")
    public boolean isRegistered(BodyParser<? extends Body> loader) {
        return isRegistered((Class<? extends BodyParser<? extends Body>>) loader.getClass());
    }

    /**
     * Checks if the given class representation of the {@link BodyParser} is registered.
     *
     * @param type The class representation of the {@link BodyParser} to check.
     * @return {@code true} when the {@link BodyParser} was registered, {@code false} otherwise.
     * @since 3.2.1-SNAPSHOT
     */
    public boolean isRegistered(Class<? extends BodyParser<? extends Body>> type) {
        if (bodyParser.isEmpty()) return false;
        return bodyParser.values().stream().anyMatch(type::isInstance);
    }

    /**
     * Retrieves the body parser associated with the specified body type.
     *
     * @param type The class representing the body type.
     * @param <T>  The type of body being parsed.
     * @return The body parser for the specified body type, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends Body> BodyParser<T> getParser(@NotNull Class<T> type) {
        return (BodyParser<T>) this.bodyParser.get(type);
    }

    /**
     * Checks if a parser is registered for the specified body type.
     *
     * @param type The class representing the body type.
     * @param <T>  The type of body being parsed.
     * @return true if a parser is registered for the specified body type, false otherwise.
     */
    public <T extends Body> boolean isParserPresent(@NotNull Class<T> type) {
        return this.bodyParser.containsKey(type);
    }

}
