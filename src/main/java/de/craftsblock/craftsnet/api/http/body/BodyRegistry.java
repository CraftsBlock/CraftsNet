package de.craftsblock.craftsnet.api.http.body;

import de.craftsblock.craftsnet.api.http.body.parser.JsonBodyParser;
import de.craftsblock.craftsnet.api.http.body.parser.MultipartFormBodyParser;
import de.craftsblock.craftsnet.api.http.body.parser.StandardFormBodyParser;
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
 * @version 1.1.0
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
        this.bodyParser.put(Objects.requireNonNull(ReflectionUtils.extractGeneric(bodyParser.getClass(), BodyParser.class)), bodyParser);
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
