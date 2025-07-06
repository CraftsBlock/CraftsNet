package de.craftsblock.craftsnet.api.http.body.parser.typed;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.bodies.typed.ByteArrayBody;

import java.io.IOException;

/**
 * This class is a body parser specifically designed to parse {@link ByteArrayBody} request bodies.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see BodyParser
 * @since 3.5.0
 */
public class ByteArrayBodyParser extends TypedBodyParser<ByteArrayBody> {

    /**
     * Constructs a new {@link ByteArrayBodyParser}.
     */
    public ByteArrayBodyParser() {
        super((request, body) -> {
            try (body) {
                return new ByteArrayBody(request, Utils.readAllBytes(body));
            } catch (IOException e) {
                throw new RuntimeException("Could not parse body to a byte[]!", e);
            }
        });
    }

}
