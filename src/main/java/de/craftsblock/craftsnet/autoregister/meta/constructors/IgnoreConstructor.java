package de.craftsblock.craftsnet.autoregister.meta.constructors;

import de.craftsblock.craftscore.annotations.Experimental;

import java.lang.annotation.*;

/**
 * This annotation is used to mark a constructor that should be ignored.
 * When applied, it indicates that the annotated constructor should not be considered
 * for automatic registration.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.5-SNAPSHOT
 */
@Documented
@Experimental
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface IgnoreConstructor {
}
