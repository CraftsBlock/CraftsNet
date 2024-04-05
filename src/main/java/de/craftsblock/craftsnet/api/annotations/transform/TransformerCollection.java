package de.craftsblock.craftsnet.api.annotations.transform;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a collection of transformer annotations.
 * This annotation is used internally and is not intended for direct use by developers.
 *
 * <p>This annotation is applied to methods or types that represent a collection of {@link Transformer} annotations.
 * It is typically used to group multiple transformation annotations together.</p>
 *
 * <p>This annotation is marked with {@link ApiStatus.Internal}, indicating that it is for internal use only
 * and should not be relied upon by external code.</p>
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Transformer
 * @since 3.0.2
 */
@ApiStatus.Internal
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TransformerCollection {

    /**
     * Specifies an array of {@link Transformer} annotations.
     *
     * @return An array of transformer annotations.
     */
    Transformer[] value();

}
