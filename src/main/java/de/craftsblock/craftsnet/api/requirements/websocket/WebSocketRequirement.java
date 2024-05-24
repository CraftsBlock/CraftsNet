package de.craftsblock.craftsnet.api.requirements.websocket;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.requirements.Requirement;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;

import java.lang.annotation.Annotation;

/**
 * Abstract class representing a websocket specific requirement.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see Requirement
 * @since 3.0.5-SNAPSHOT
 */
public abstract class WebSocketRequirement extends Requirement<WebSocketClient, RouteRegistry.EndpointMapping> {

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
