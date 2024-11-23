package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.http.body.Body;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;

import java.lang.annotation.*;

/**
 * Annotation used to indicate that a request handler method requires specific types of request bodies.
 * This annotation can be applied to methods or classes to specify the required body types.
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
public @interface RequireBody {

    /**
     * The classes representing the types of bodies that are required for the annotated method or class.
     *
     * @return An array of body types that are required.
     */
    @RequirementStore
    Class<? extends Body>[] value();

}
