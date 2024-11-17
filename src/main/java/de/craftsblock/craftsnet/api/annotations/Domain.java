package de.craftsblock.craftsnet.api.annotations;

import de.craftsblock.craftsnet.api.http.WebServer;

import java.lang.annotation.*;

/**
 * Specifies the domain associated with an HTTP request handler or service.
 * This annotation can be applied to methods or classes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see WebServer
 * @since CraftsNet-2.3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Domain {

    /**
     * Defines the domain associated with the annotated method or class.
     *
     * @return The domain as a string.
     */
    String[] value();

}
