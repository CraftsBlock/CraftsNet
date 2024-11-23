package de.craftsblock.craftsnet.api.requirements.meta;

import de.craftsblock.craftscore.annotations.Experimental;

import java.lang.annotation.*;

/**
 * Annotation to mark methods within a requirement annotation for value storage.
 *
 * <p>When applied to a method, this indicates that the method's return value
 * should be used for storing specific requirement-related data. This is
 * particularly useful in conjunction with {@link RequirementMeta} annotations
 * for defining custom requirements.</p>
 *
 * <p>Methods annotated with {@link RequirementStore} are automatically
 * detected and their values are extracted during requirement processing.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see RequirementMeta
 * @since 3.0.7-SNAPSHOT
 */
@Documented
@Experimental
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirementStore {
}
