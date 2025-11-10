package de.craftsblock.craftsnet.api.http.body.parser.typed;

import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.ContentType;
import de.craftsblock.craftsnet.api.http.body.bodies.JsonBody;

import java.io.IOException;

/**
 * This class is a body parser specifically designed to parse json request bodies.
 * It extends the {@link BodyParser} class and provides an implementation to parse json data from an input stream.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.0
 * @see BodyParser
 * @since 3.0.4-SNAPSHOT
 */
public class JsonBodyParser extends TypedBodyParser<JsonBody> {

    /**
     * Constructs a new JsonBodyParser object with the content type set to "application/json".
     */
    public JsonBodyParser() {
        super((request, body) -> {
            try (body) {
                return new JsonBody(request, JsonParser.parse(body));
            } catch (IOException e) {
                throw new RuntimeException("Could not parse body to an json object!", e);
            }
        }, ContentType.APPLICATION_JSON, "text/.*");
    }

}
