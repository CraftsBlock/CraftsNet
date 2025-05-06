package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.Transformable;
import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;

/**
 * A transformer class for converting a string representation of a long value to a Long object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see Transformable
 * @since 3.0.2-SNAPSHOT
 */
public class LongTransformer implements Transformable<Long, String> {

    /**
     * Transforms the provided string parameter into a Long object.
     *
     * @param parameter The string representation of the long value.
     * @return The Long object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to a Long.
     */
    @Override
    public Long transform(String parameter) {
        try {
            return Long.parseLong(parameter);
        } catch (NumberFormatException e) {
            throw new NotTransformableException(parameter, Long.class);
        }
    }

}