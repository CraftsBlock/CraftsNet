package de.craftsblock.craftsnet.api.http.body.parser.typed;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.bodies.typed.StringBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class is a body parser specifically designed to parse {@link StringBody} request bodies.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see BodyParser
 * @since 3.5.0
 */
public class StringBodyParser extends TypedBodyParser<StringBody> {

    /**
     * Constructs a new {@link StringBodyParser}.
     */
    public StringBodyParser() {
        super((request, body) -> {
            try (body) {
                byte[] data = Utils.readAllBytes(body);
                return new StringBody(request, new String(data, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Could not parse body to an string!", e);
            }
        });
    }
}
