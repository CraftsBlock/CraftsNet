package de.craftsblock.craftsnet.addon.meta.annotations;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A container annotation for grouping multiple {@link Depends} annotations.
 * <p>
 * The {@link DependsCollection} annotation is used internally to aggregate multiple {@link Depends} annotations
 * when the {@link Depends} annotation is applied repeatedly to a single type. This annotation should not be used
 * directly by developers but is automatically generated by the compiler.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Depends
 * @since 3.1.0-SNAPSHOT
 */
@ApiStatus.Internal
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DependsCollection {

    /**
     * An array of {@link Depends} annotations.
     * <p>
     * This array contains all {@link Depends} annotations applied to a single type.
     * It is automatically populated when {@link Depends} is used repeatedly.
     * </p>
     *
     * @return The grouped {@link Depends} annotations.
     */
    Depends[] value();

}
