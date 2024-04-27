package de.craftsblock.craftsnet.api.http.body;

import de.craftsblock.craftsnet.api.http.Request;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This abstract class represents a parser for HTTP request or response bodies.
 * It provides methods to parse a body from an input stream and to check if a specific content type is parseable.
 *
 * @param <T> The type of body that this parser can parse.
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Body
 * @see BodyRegistry
 * @since CraftsNet-3.0.4
 */
public abstract class BodyParser<T extends Body> {

    private final List<String> contentTypes;

    /**
     * Constructs a new BodyParser with the given content types.
     *
     * @param contentType  The primary content type supported by this parser.
     * @param contentTypes Additional content types supported by this parser.
     */
    public BodyParser(String contentType, String... contentTypes) {
        List<String> types = new ArrayList<>();
        types.add(contentType);
        types.addAll(List.of(contentTypes));
        this.contentTypes = Collections.unmodifiableList(types);
    }

    /**
     * Parses the body from the provided input stream.
     *
     * @param request The HTTP request associated with the body.
     * @param body    The input stream containing the body data.
     * @return The parsed body object, or null if parsing fails.
     */
    public abstract @Nullable T parse(Request request, InputStream body);

    /**
     * Returns an unmodifiable list of supported content types.
     *
     * @return The list of supported content types.
     */
    public final @Unmodifiable List<String> contentTypes() {
        return contentTypes;
    }

    /**
     * Checks if this parser can parse the given content type.
     *
     * @param contentType The content type to check.
     * @return true if the parser supports the given content type, false otherwise.
     */
    public final boolean isParseable(String contentType) {
        return this.contentTypes.contains(contentType);
    }

}
