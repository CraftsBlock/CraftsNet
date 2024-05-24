package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.requirements.Requirement;

import java.lang.annotation.Annotation;

/**
 * Abstract class representing a web specific requirement.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Requirement
 * @since 3.0.5-SNAPSHOT
 */
public abstract class WebRequirement extends Requirement<Request, RouteRegistry.RouteMapping> {

    /**
     * Constructs a new web requirement with the specified annotation class. The annotation will
     * be used to determine the requirement on the methods of endpoints.
     *
     * @param annotation the annotation class that this requirement is associated with
     */
    public WebRequirement(Class<? extends Annotation> annotation) {
        super(annotation);
    }

}
