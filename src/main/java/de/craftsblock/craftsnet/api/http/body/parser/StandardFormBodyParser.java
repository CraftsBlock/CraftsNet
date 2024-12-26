package de.craftsblock.craftsnet.api.http.body.parser;

import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.ContentType;
import de.craftsblock.craftsnet.api.http.body.bodies.StandardFormBody;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is a body parser specifically designed to parse standard form request bodies.
 * It extends the {@link BodyParser} class and provides an implementation to parse standard form from an input stream.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see BodyParser
 * @since 3.0.4-SNAPSHOT
 */
public class StandardFormBodyParser extends BodyParser<StandardFormBody> {

    /**
     * Constructs a new StandardFormBodyParser object with the content type set to "application/x-www-form-urlencoded".
     */
    public StandardFormBodyParser() {
        super(ContentType.APPLICATION_FORM_URLENCODED);
    }

    /**
     * Parses the standard form body from the provided input stream.
     *
     * @param request The HTTP request associated with the body.
     * @param body    The input stream containing the standard form body.
     * @return The parsed standard form body object, or null if parsing fails.
     */
    @Override
    public @Nullable StandardFormBody parse(Request request, InputStream body) {
        try {
            return new StandardFormBody(request, body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
