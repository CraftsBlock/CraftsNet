package de.craftsblock.craftsnet.api.http.body.parser.typed;

import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.bodies.typed.TypedBody;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.function.BiFunction;

/**
 * This class is a body parser specifically designed to parse {@link TypedBody} request bodies.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see BodyParser
 * @since 3.5.0
 */
public abstract class TypedBodyParser<T extends TypedBody<?>> extends BodyParser<T> {

    private final BiFunction<Request, InputStream, T> factory;

    /**
     * Constructs a new {@link TypedBodyParser}.
     *
     * @param factory A {@link BiFunction} which is used to created new bodies.
     */
    public TypedBodyParser(BiFunction<Request, InputStream, T> factory) {
        this(factory, ".*");
    }

    /**
     * Constructs a new {@link TypedBodyParser} with the given content types.
     *
     * @param factory      A {@link BiFunction} which is used to created new bodies.
     * @param contentType  The primary content type supported by this parser.
     * @param contentTypes Additional content types supported by this parser.
     */
    public TypedBodyParser(BiFunction<Request, InputStream, T> factory, String contentType, String... contentTypes) {
        super(contentType, contentTypes);
        this.factory = factory;
    }

    /**
     * Parses the body from the provided input stream.
     *
     * @param request The http request associated with the body.
     * @param body    The input stream containing the body data.
     * @return The parsed body object, or null if parsing fails.
     */
    @Override
    public @Nullable T parse(Request request, InputStream body) {
        return factory.apply(request, body);
    }

}
