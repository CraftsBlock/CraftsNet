package de.craftsblock.craftsnet.api.transformers;

import de.craftsblock.craftsnet.api.exceptions.NotTransformableException;
import de.craftsblock.craftsnet.api.interfaces.Transformable;

/**
 * A transformer class for converting a string representation of a long value to a Long object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformable
 * @since 3.0.2
 */
public class LongTransformer implements Transformable<Long> {

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