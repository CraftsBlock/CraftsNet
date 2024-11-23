package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;

import java.lang.annotation.*;

/**
 * This annotation can be applied to methods or types to indicate that
 * certain content types are required for processing the request.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @since 3.0.4-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@RequirementMeta(type = RequirementType.STORING)
public @interface RequireContentType {

    /**
     * Specifies the required content types.
     *
     * @return An array of strings representing the names of the content types.
     */
    @RequirementStore
    String[] value();

}
