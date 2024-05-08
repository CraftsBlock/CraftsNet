package de.craftsblock.craftsnet.api.http.body.parser;

import de.craftsblock.craftscore.utils.Validator;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.ContentType;
import de.craftsblock.craftsnet.api.http.body.bodies.JsonBody;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is a body parser specifically designed to parse json request bodies.
 * It extends the {@link BodyParser} class and provides an implementation to parse json data from an input stream.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see BodyParser
 * @since CraftsNet-3.0.4
 */
public class JsonBodyParser extends BodyParser<JsonBody> {

    /**
     * Constructs a new JsonBodyParser object with the content type set to "application/json".
     */
    public JsonBodyParser() {
        super(ContentType.APPLICATION_JSON, "text/*");
    }

    /**
     * Parses the json body from the provided input stream.
     *
     * @param request The HTTP request associated with the body.
     * @param body    The input stream containing the json body data.
     * @return The parsed json body object, or null if parsing fails.
     */
    @Override
    public @Nullable JsonBody parse(Request request, InputStream body) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body))) {
            StringBuilder lines = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                lines.append(line).append("\n");
            reader.close();
            if (Validator.isJsonValid(lines.toString()))
                return new JsonBody(request, lines.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
