package de.craftsblock.craftsnet.api.http.annotations;

import de.craftsblock.craftsnet.api.http.body.Body;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that a request handler method requires specific types of request bodies.
 * This annotation can be applied to methods or classes to specify the required body types.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-3.0.4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireBody {

    /**
     * The classes representing the types of bodies that are required for the annotated method or class.
     *
     * @return An array of body types that are required.
     */
    Class<? extends Body>[] value();

}
