package de.craftsblock.craftsnet.api.transformers;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Interface representing a transformation operation.
 * Classes implementing this interface are capable of transforming a given parameter
 * into a specific type.
 *
 * @param <T> The type from which the transformer transforms the param.
 * @param <R> The type to which the parameter is transformed.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.2-SNAPSHOT
 */
@FunctionalInterface
public interface Transformable<T, R> extends Function<T, R> {

    /**
     * Transforms the provided parameter into a specific type.
     *
     * @param parameter The parameter to be transformed.
     * @return The transformed parameter of type {@link T}.
     */
    R transform(T parameter);

    /**
     * Delegates to {@link #transform(Object)}.
     *
     * @param parameter the function argument
     * @return The transformed parameter.
     */
    @Override
    default R apply(T parameter) {
        return this.transform(parameter);
    }

    /**
     * Gets the parent {@link Transformable transformer} which is invoked before invoking
     * {@link #transform(Object)} on this {@link Transformable transformer}.
     * If the parent is null, no parent {@link Transformable transformer} will be applied
     * before invoking {@link #transform(Object)}.
     *
     * @return The parent {@link Transformable transformer}, may be null.
     * @since 3.4.0-SNAPSHOT
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
