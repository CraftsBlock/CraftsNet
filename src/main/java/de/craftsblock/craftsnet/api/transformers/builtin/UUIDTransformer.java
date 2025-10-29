package de.craftsblock.craftsnet.api.transformers.builtin;

import de.craftsblock.craftsnet.api.transformers.Transformable;
import de.craftsblock.craftsnet.api.transformers.exceptions.NotTransformableException;

import java.util.UUID;

/**
 * A transformer class for converting a string representation of an uuid value to an {@link UUID} object.
 * Implements the {@link Transformable} interface.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformable
 * @since 3.5.5
 */
public class UUIDTransformer implements Transformable<UUID, String> {

    /**
     * Transforms the provided string parameter into an {@link UUID} object.
     *
     * @param parameter The string representation of the uuid value.
     * @return The {@link UUID} object representing the transformed value.
     * @throws NotTransformableException If the parameter cannot be transformed to an {@link UUID}.
     */
    @Override
    public UUID transform(String parameter) {
        try {
            return UUID.fromString(parameter);
        } catch (IllegalArgumentException e) {
            throw new NotTransformableException(parameter, UUID.class);
        }
    }

}
