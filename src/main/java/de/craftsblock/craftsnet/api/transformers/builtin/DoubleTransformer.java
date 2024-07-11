package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.Transformable;
import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;

/**
 * A transformer class for converting a string representation of a double value to a Double object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformable
 * @since CraftsNet-3.0.2
 */
public class DoubleTransformer implements Transformable<Double> {

    /**
     * Transforms the provided string parameter into a Double object.
     *
     * @param parameter The string representation of the double value.
     * @return The Double object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to a Double.
     */
    @Override
    public Double transform(String parameter) {
        try {
            return Double.parseDouble(parameter);
        } catch (NumberFormatException e) {
            throw new NotTransformableException(parameter, Double.class);
        }
    }

}
