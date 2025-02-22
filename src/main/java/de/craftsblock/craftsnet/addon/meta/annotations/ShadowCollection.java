package de.craftsblock.craftsnet.addon.meta.annotations;

import de.craftsblock.craftscore.annotations.Experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for grouping multiple {@link Shadow} annotations.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Shadow
 * @since 3.3.4-SNAPSHOT
 */
@Experimental
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShadowCollection {

    /**
     * An array of {@link Shadow} annotations.
     *
     * @return the array of shadow annotations
     */
    Shadow[] value();

}
