package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;
import de.craftsblock.craftsnet.api.transformers.Transformable;

/**
 * A transformer class for converting a string representation of a boolean value to a Boolean object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformable
 * @since 3.0.2
 */
public class BooleanTransformer implements Transformable<Boolean> {

    /**
     * Transforms the provided string parameter into a Boolean object.
     *
     * @param parameter The string representation of the boolean value.
     * @return The Boolean object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to a Boolean.
     */
    @Override
    public Boolean transform(String parameter) {
        try {
            return Boolean.parseBoolean(parameter);
        } catch (NumberFormatException e) {
            throw new NotTransformableException(parameter, Boolean.class);
        }
    }

}
