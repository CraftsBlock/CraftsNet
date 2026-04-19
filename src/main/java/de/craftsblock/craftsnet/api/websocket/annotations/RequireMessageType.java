package de.craftsblock.craftsnet.api.websocket.annotations;

import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementMeta;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementStore;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;
import de.craftsblock.craftsnet.api.websocket.Opcode;

import java.lang.annotation.*;

/**
 * Specifies the message type that should be received.
 * This annotation can be applied to methods or classes.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see WebServer
 * @since 3.0.5-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@RequirementMeta(type = RequirementType.STORING)
public @interface RequireMessageType {

    /**
     * Defines the message types associated with the annotated method or class.
     *
     * @return The domain as a string.
     */
    @RequirementStore
    Opcode[] value() default {Opcode.TEXT, Opcode.BINARY};

}
