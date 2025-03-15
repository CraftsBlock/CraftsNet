package de.craftsblock.craftsnet.autoregister.meta.constructors;

import de.craftsblock.craftscore.annotations.Experimental;

import java.lang.annotation.*;

/**
 * This annotation is used to mark a constructor as a fallback constructor.
 * A fallback constructor is intended to be used when no other suitable constructors are available.
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
public @interface FallbackConstructor {
}
