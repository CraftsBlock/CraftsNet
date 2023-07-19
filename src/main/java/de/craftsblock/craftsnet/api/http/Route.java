package de.craftsblock.craftsnet.api.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Route annotation is used to mark methods that serve as handlers for specific API endpoints in the RouteRegistry.
 *
 * @author CraftsBlock
 * @see WebServer
 * @see RequestMethod
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Route {

    /**
     * Specifies the URL path for the API endpoint.
     *
     * @return The URL path for the API endpoint.
     */
    String path();

    /**
     * Specifies the HTTP methods that the API endpoint should respond to.
     * Default methods include POST and GET.
     *
     * @return The HTTP methods that the API endpoint should respond to.
     */
    RequestMethod[] methods() default {RequestMethod.POST, RequestMethod.GET};

}
