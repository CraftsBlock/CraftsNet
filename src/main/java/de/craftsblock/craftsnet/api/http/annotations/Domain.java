package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.http.WebServer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the domain associated with an HTTP request handler or service.
 * This annotation can be applied to methods or classes.
 *
 * @author CraftsBlock
 * @version 1.0
 * @see WebServer
 * @since 2.3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Domain {

    /**
     * Defines the domain associated with the annotated method or class.
     *
     * @return The domain as a string.
     */
    String[] value() default {"*"};

}
