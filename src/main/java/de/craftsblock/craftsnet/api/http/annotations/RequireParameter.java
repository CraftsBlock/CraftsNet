package de.craftsblock.craftsnet.api.http.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required url parameters for methods or types.
 * This annotation can be applied to methods or types to indicate that
 * certain url parameter are required for processing the request.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.6-SNAPSHOT
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireParameter {

    /**
     * Specifies the required url parameter.
     *
     * @return An array of strings representing the names of the required url parameter.
     */
    String[] value();

}
