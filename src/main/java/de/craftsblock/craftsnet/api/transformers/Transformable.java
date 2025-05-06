package de.craftsblock.craftsnet.api.transformers;

import org.jetbrains.annotations.Nullable;

/**
 * Interface representing a transformation operation.
 * Classes implementing this interface are capable of transforming a given parameter
 * into a specific type.
 *
 * @param <R> The type to which the parameter is transformed.
 * @param <T> The type from which the transformer transforms the param.
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @since 3.0.2-SNAPSHOT
 */
public interface Transformable<R, T> {

    /**
     * Transforms the provided parameter into a specific type.
     *
     * @param parameter The parameter to be transformed.
     * @return The transformed parameter of type T.
     */
    R transform(T parameter);

    /**
     * Gets the parent {@link Transformable transformer} which is invoked before invoking
     * {@link #transform(Object)} on this {@link Transformable transformer}.
     * If the parent is null, no parent {@link Transformable transformer} will be applied
     * before invoking {@link #transform(Object)}.
     *
     * @return The parent {@link Transformable transformer}, may be null.
     * @since 3.3.6-SNAPSHOT
     */
    default @Nullable Class<? extends Transformable<T, ?>> getParent() {
        return null;
    }

    /**
     * Gets whether the result of {@link #transform(Object)} should be cached or not.
     *
     * @return true when its cacheable, false otherwise.
     */
    default boolean isCacheable() {
        return true;
    }

}
