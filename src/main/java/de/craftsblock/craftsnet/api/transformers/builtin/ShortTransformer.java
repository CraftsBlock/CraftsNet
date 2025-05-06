package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.Transformable;
import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;

/**
 * A transformer class for converting a string representation of a short value to a Short object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see Transformable
 * @since 3.0.2-SNAPSHOT
 */
public class ShortTransformer implements Transformable<Short, String> {

    /**
     * Transforms the provided string parameter into a Short object.
     *
     * @param parameter The string representation of the short value.
     * @return The Short object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to a Short.
     */
    @Override
    public Short transform(String parameter) {
        try {
            return Short.parseShort(parameter);
        } catch (NumberFormatException e) {
            throw new NotTransformableException(parameter, Short.class);
        }
    }

}
