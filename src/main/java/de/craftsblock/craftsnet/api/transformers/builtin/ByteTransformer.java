package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.Transformable;
import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;

/**
 * A transformer class for converting a string representation of a byte value to a Byte object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see Transformable
 * @since 3.0.2-SNAPSHOT
 */
public class ByteTransformer implements Transformable<Byte, String> {

    /**
     * Transforms the provided string parameter into a Byte object.
     *
     * @param parameter The string representation of the byte value.
     * @return The Byte object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to a Byte.
     */
    @Override
    public Byte transform(String parameter) {
        try {
            return Byte.parseByte(parameter);
        } catch (NumberFormatException e) {
            throw new NotTransformableException(parameter, Byte.class);
        }
    }

}
