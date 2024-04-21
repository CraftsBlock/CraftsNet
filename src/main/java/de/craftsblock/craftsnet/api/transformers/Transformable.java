package de.craftsblock.craftsnet.api.transformers;

/**
 * Interface representing a transformation operation.
 * Classes implementing this interface are capable of transforming a given parameter
 * into a specific type.
 *
 * @param <T> The type to which the parameter is transformed.
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
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

    /**
     * Gets whether the result of {@link Transformable#transform(String)} should be cached or not.
     *
     * @return true when its cacheable, false otherwise.
     */
    default boolean isCacheable() {
        return true;
    }

}
