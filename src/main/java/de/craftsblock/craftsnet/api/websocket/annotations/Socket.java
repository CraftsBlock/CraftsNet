package de.craftsblock.craftsnet.api.websocket.annotations;

import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;
import de.craftsblock.craftsnet.api.websocket.SocketHandler;

import java.lang.annotation.*;

/**
 * Custom annotation used to mark classes as WebSocket handlers.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see SocketHandler
 * @since 2.1.1-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@RequirementMeta(type = RequirementType.STORING)
public @interface Socket {

    /**
     * Specifies the WebSocket path to which this handler is associated.
     * This path is used to identify which WebSocket endpoint the handler is responsible for.
     *
     * @return The WebSocket path associated with this handler.
     */
    @RequirementStore
    String value() default "/";


}
