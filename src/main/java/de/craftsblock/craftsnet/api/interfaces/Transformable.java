package de.craftsblock.craftsnet.api.interfaces;

/**
 * Interface representing a transformation operation.
 * Classes implementing this interface are capable of transforming a given parameter
 * into a specific type.
 *
 * @param <T> The type to which the parameter is transformed.
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.2
 */
public interface Transformable<T> {

    /**
     * Transforms the provided parameter into a specific type.
     *
     * @param parameter The parameter to be transformed.
     * @return The transformed parameter of type T.
     */
    T transform(String parameter);

}
