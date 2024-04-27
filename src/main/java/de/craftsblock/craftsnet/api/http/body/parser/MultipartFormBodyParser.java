package de.craftsblock.craftsnet.api.http.body.parser;

import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.ContentType;
import de.craftsblock.craftsnet.api.http.body.bodies.MultipartFormBody;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is a body parser specifically designed to parse multipart form data request bodies.
 * It extends the {@link BodyParser} class and provides an implementation to parse multipart form data from an input stream.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see BodyParser
 * @since CraftsNet-3.0.4
 */
public class MultipartFormBodyParser extends BodyParser<MultipartFormBody> {

    /**
     * Constructs a new MultipartFormBodyParser object with the content type set to "multipart/form-data".
     */
    public MultipartFormBodyParser() {
        super(ContentType.MULTIPART_FORM_DATA);
    }

    /**
     * Parses the multipart form data body from the provided input stream.
     *
     * @param request The HTTP request associated with the body.
     * @param body    The input stream containing the multipart form data body.
     * @return The parsed multipart form data body object, or null if parsing fails.
     */
    @Override
    public @Nullable MultipartFormBody parse(Request request, InputStream body) {
        String[] boundary = request.getContentType().split("=");
        // Ensure that the Content-Type header contains a valid boundary
        if (boundary.length != 2) return null;
        try {
            return new MultipartFormBody(request, boundary[1], body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
