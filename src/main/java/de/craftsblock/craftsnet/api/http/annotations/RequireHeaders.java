package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;

import java.lang.annotation.*;

/**
 * Annotation to specify required HTTP headers for methods or types.
 * This annotation can be applied to methods or types to indicate that
 * certain HTTP headers are required for processing the request.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @since 3.0.2-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@RequirementMeta(type = RequirementType.STORING)
public @interface RequireHeaders {

    /**
     * Specifies the required HTTP headers.
     *
     * @return An array of strings representing the names of the required headers.
     */
    @RequirementStore
    String[] value();

}
