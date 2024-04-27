package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.http.WebServer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the path associated with a service method or defines the parent path.
 * This annotation can be applied to methods or classes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see WebServer
 * @see HttpMethod
 * @since CraftsNet-1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Route {

    /**
     * Defines the path associated with the annotated method or class.
     *
     * @return The path as a string.
     */
    String value() default "/";

}
