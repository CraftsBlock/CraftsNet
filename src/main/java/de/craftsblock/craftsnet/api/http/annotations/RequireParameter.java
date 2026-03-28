package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;

import java.lang.annotation.*;

/**
 * Annotation to specify required url parameters for methods or types.
 * This annotation can be applied to methods or types to indicate that
 * certain url parameter are required for processing the request.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.6-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@RequirementMeta(type = RequirementType.STORING)
public @interface RequireParameter {

    /**
     * Specifies the required url parameter.
     *
     * @return An array of strings representing the names of the required url parameter.
     */
    @RequirementStore
    String[] value();

}
