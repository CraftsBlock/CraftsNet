package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.http.HttpMethod;

import java.lang.annotation.*;

/**
 * Specifies the HTTP request methods that are supported by the service method.
 * This annotation can be applied to methods or classes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see Route
 * @see HttpMethod
 * @since CraftsNet-2.3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequestMethod {

    /**
     * Defines the HTTP request methods that are supported. By default, it includes POST and GET methods.
     *
     * @return An array of HTTP methods.
     */
    HttpMethod[] value() default {HttpMethod.POST, HttpMethod.GET};

}
