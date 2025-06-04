package de.craftsblock.craftsnet.addon.meta.annotations;

import de.craftsblock.craftsnet.addon.Addon;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.*;

/**
 * Declares a dependency for an {@link Addon}.
 * <p>
 * The {@link Depends} annotation is used to specify that an addon depends on another addon for its functionality.
 * It indicates the required addon by referencing its class type. This annotation can be applied multiple times
 * to declare multiple dependencies.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @see DependsCollection
 * @since 3.1.0-SNAPSHOT
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DependsCollection.class)
public @interface Depends {

    /**
     * Specifies the class of the required addon.
     * <p>
     * This class must extend {@link Addon} and represents the addon that the annotated addon depends on.
     * The dependency will be resolved during the addon initialization process.
     * </p>
     *
     * @return The class of the required addon.
     */
    Class<? extends Addon> value();

    /**
     * Specifies whether this dependency should be treated as optional or not.
     * {@code true} means that this dependency is optional, {@code false} means
     * the opposite. The default value is {@code false}.
     *
     * @return {@code true} if this dependency is optional, {@code false} otherwise.
     * @since 3.3.4-SNAPSHOT
     */
    boolean soft() default false;

}
