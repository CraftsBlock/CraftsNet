package de.craftsblock.craftsnet.api.annotations.transform;

import de.craftsblock.craftsnet.api.interfaces.Transformable;

import java.lang.annotation.*;

/**
 * Annotation to mark methods or types that provide transformation functionality.
 * This annotation specifies various attributes including the parameter name, the transformer class,
 * and the caching behavior for the transformation operation.
 *
 * <p>Methods or types annotated with {@code @Transformer} are intended to facilitate the transformation
 * of input parameters into desired output types. This can be particularly useful in scenarios where
 * data needs to be converted between different formats or representations.</p>
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformable
 * @see TransformerCollection
 * @since 3.0.2
 */
@Repeatable(TransformerCollection.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transformer {

    /**
     * Specifies the name of the parameter to be transformed.
     *
     * @return The name of the parameter.
     */
    String parameter();

    /**
     * Specifies the class of the transformer that performs the transformation.
     * The transformer class must implement the {@link Transformable} interface.
     *
     * @return The class of the transformer.
     */
    Class<? extends Transformable<?>> transformer();

    /**
     * Specifies whether caching is enabled for the transformation.
     * If set to {@code true}, the transformation result may be cached for subsequent invocations.
     *
     * @return True if caching is enabled, false otherwise.
     */
    boolean cacheable() default true;

}
