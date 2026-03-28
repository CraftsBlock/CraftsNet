package de.craftsblock.craftsnet.autoregister.meta.constructors;

import java.lang.annotation.*;

/**
 * This annotation is used to mark a constructor as the preferred constructor.
 * When multiple constructors are available, this annotation indicates
 * that the annotated constructor should be prioritized for the automatic
 * registration.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.3.5-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface PreferConstructor {
}
