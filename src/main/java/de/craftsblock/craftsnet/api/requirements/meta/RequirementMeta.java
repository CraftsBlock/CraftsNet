package de.craftsblock.craftsnet.api.requirements.meta;

import java.lang.annotation.*;

/**
 * Annotation to define metadata for custom requirement annotations.
 *
 * <p>This meta-annotation is applied to other annotations that serve
 * as requirements within the system. It provides details about how
 * the annotated requirement should be handled, such as the methods
 * to extract data and its type.</p>
 *
 * <p>Attributes:</p>
 * <ul>
 *     <li>{@code methods}: Specifies which method(s) to use when retrieving
 *     data from the annotated requirement.</li>
 *     <li>{@code type}: Indicates the type of requirement. Defaults to
 *     {@link RequirementType#STORING}.</li>
 * </ul>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see RequirementType
 * @since 3.1.0-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@RequirementMeta
public @interface RequirementMeta {

    /**
     * Specifies the method(s) to be used when extracting values from
     * the annotated requirement.
     *
     * @return an array of method names to extract values from.
     */
    String[] methods() default "value";

    /**
     * Defines the type of requirement. This affects how the requirement is interpreted and processed.
     *
     * <p>Defaults to {@link RequirementType#STORING}, which assumes that
     * the requirement holds specific data to be stored and processed.</p>
     *
     * @return the {@link RequirementType} of the annotated requirement.
     */
    RequirementType type() default RequirementType.STORING;

}
