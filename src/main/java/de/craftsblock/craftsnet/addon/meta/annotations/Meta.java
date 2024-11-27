package de.craftsblock.craftsnet.addon.meta.annotations;

import de.craftsblock.craftscore.annotations.Experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides basic metadata for {@link de.craftsblock.craftsnet.addon.Addon}.
 * <p>
 * The {@link Meta} annotation is used to define basic information about an addon,
 * such as its name, which is used in various parts of the addon management system.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
@Experimental
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Meta {

    /**
     * The name of the addon.
     * <p>
     * This name must be unique and should adhere to the addon naming conventions
     * (e.g., no special characters or spaces).
     * </p>
     *
     * @return The name of the addon.
     */
    String name();

}
