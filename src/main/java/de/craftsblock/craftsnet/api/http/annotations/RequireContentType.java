package de.craftsblock.craftsnet.api.http.annotations;

import java.lang.annotation.*;

/**
 * This annotation can be applied to methods or types to indicate that
 * certain content types are required for processing the request.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireContentType {

    /**
     * Specifies the required content types.
     *
     * @return An array of strings representing the names of the content types.
     */
    String[] value();

}
