package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;
import de.craftsblock.craftsnet.api.transformers.Transformable;

/**
 * A transformer class for converting a string representation of a float value to a Float object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformable
 * @since 3.0.2
 */
public class FloatTransformer implements Transformable<Float> {

    /**
     * Transforms the provided string parameter into a Float object.
     *
     * @param parameter The string representation of the float value.
     * @return The Float object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to a Float.
     */
    @Override
    public Float transform(String parameter) {
        try {
            return Float.parseFloat(parameter);
        } catch (NumberFormatException e) {
            throw new NotTransformableException(parameter, Float.class);
        }
    }

}