package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;

import java.lang.annotation.*;

/**
 * Specifies the HTTP request methods that are supported by the service method.
 * This annotation can be applied to methods or classes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see Route
 * @see HttpMethod
 * @since 2.3.0-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@RequirementMeta(type = RequirementType.STORING)
public @interface RequestMethod {

    /**
     * Defines the HTTP request methods that are supported. By default, it includes POST and GET methods.
     *
     * @return An array of HTTP methods.
     */
    @RequirementStore
    HttpMethod[] value() default {HttpMethod.POST, HttpMethod.GET};

}
