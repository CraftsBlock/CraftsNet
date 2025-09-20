package de.craftsblock.craftsnet.api.requirements.websocket;

import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.requirements.Requirement;

import java.lang.annotation.Annotation;

/**
 * Abstract class representing a websocket specific requirement.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see Requirement
 * @since 3.0.5-SNAPSHOT
 */
public abstract class WebSocketRequirement<T extends RequireAble> extends Requirement<T> {

    /**
     * Constructs a new websocket requirement with the specified annotation class. The annotation will
     * be used to determine the requirement on the methods of endpoints.
     *
     * @param annotation the annotation class that this requirement is associated with
     */
    public WebSocketRequirement(Class<? extends Annotation> annotation) {
        super(annotation);
    }

}
