package de.craftsblock.craftsnet.api.http.annotations;

import java.lang.annotation.*;

/**
 * Annotation to specify required HTTP headers for methods or types.
 * This annotation can be applied to methods or types to indicate that
 * certain HTTP headers are required for processing the request.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-3.0.2
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireHeaders {

    /**
     * Specifies the required HTTP headers.
     *
     * @return An array of strings representing the names of the required headers.
     */
    String[] value();

}
