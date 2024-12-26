package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.Transformable;
import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;

/**
 * A transformer class for converting a string representation of an integer value to an Integer object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformable
 * @since 3.0.2-SNAPSHOT
 */
public class IntTransformer implements Transformable<Integer> {

    /**
     * Transforms the provided string parameter into an Integer object.
     *
     * @param parameter The string representation of the integer value.
     * @return The Integer object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to an Integer.
     */
    @Override
    public Integer transform(String parameter) {
        try {
            return Integer.parseInt(parameter);
        } catch (NumberFormatException e) {
            throw new NotTransformableException(parameter, Integer.class);
        }
    }

}
