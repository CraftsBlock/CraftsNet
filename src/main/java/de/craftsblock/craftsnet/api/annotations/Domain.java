package de.craftsblock.craftsnet.api.annotations;

import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;

import java.lang.annotation.*;

/**
 * Specifies the domain associated with an HTTP request handler or service.
 * This annotation can be applied to methods or classes.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see WebServer
 * @since 2.3.0-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@RequirementMeta(type = RequirementType.STORING)
public @interface Domain {

    /**
     * Defines the domain associated with the annotated method or class.
     *
     * @return The domain as a string.
     */
    @RequirementStore
    String[] value();

}
