package de.craftsblock.craftsnet.addon.meta.annotations;

import de.craftsblock.craftsnet.addon.meta.ShadowType;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.*;

/**
 * Indicates that the annotated {@link de.craftsblock.craftsnet.addon.Addon} requires a dependency
 * or repository.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see ShadowCollection
 * @see ShadowType
 * @since 3.3.4-SNAPSHOT
 */
@ApiStatus.Experimental
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ShadowCollection.class)
public @interface Shadow {

    /**
     * Specifies the dependency or repository.
     *
     * @return the dependency or repository
     */
    String value();

    /**
     * Specifies the type of the shadow dependency.
     * Defaults to {@link ShadowType#DEPENDENCY} if not explicitly defined.
     *
     * @return the type of the shadow dependency
     */
    ShadowType type() default ShadowType.DEPENDENCY;

}
