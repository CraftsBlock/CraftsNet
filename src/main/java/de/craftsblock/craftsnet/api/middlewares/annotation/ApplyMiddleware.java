package de.craftsblock.craftsnet.api.middlewares.annotation;

import de.craftsblock.craftsnet.api.middlewares.Middleware;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Annotation to set specific {@link Middleware middlewares} to be used before the endpoint
 * is performed.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.6-SNAPSHOT
 */
@Documented
@Repeatable(ApplyMiddleware.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ApplyMiddleware {

    /**
     * Gets the type of the {@link Middleware} that should be applied before
     * running the endpoint.
     *
     * @return The type of the {@link Middleware} that should be applied.
     */
    Class<? extends Middleware>[] value();

    /**
     * The {@link List} annotation is used to repeat the {@link ApplyMiddleware}
     * annotation.
     *
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.3.6-SNAPSHOT
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {

        /**
         * Specifies an array of {@link ApplyMiddleware} annotations.
         *
         * @return An array of the {@link ApplyMiddleware} instances.
         */
        ApplyMiddleware @NotNull [] value();

    }

}
