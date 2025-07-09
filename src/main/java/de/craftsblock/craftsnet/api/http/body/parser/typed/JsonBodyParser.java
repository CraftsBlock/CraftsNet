package de.craftsblock.craftsnet.api.http.body.parser.typed;

import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftscore.json.JsonValidator;
import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.ContentType;
import de.craftsblock.craftsnet.api.http.body.bodies.JsonBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class is a body parser specifically designed to parse json request bodies.
 * It extends the {@link BodyParser} class and provides an implementation to parse json data from an input stream.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
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
                byte[] data = Utils.readAllBytes(body);
                String json = new String(data, StandardCharsets.UTF_8);

                if (!JsonValidator.isValid(json)) return null;
                return new JsonBody(request, JsonParser.parse(json));
            } catch (IOException e) {
                throw new RuntimeException("Could not parse body to an json object!", e);
            }
        }, ContentType.APPLICATION_JSON, "text/.*");
    }

}
