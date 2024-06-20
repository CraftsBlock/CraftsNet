package de.craftsblock.craftsnet.api.websocket.annotations;

import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.websocket.Opcode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the message type that should be received.
 * This annotation can be applied to methods or classes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see WebServer
 * @since 3.0.5-SNAPSHOT
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireMessageType {

    /**
     * Defines the message types associated with the annotated method or class.
     *
     * @return The domain as a string.
     */
    Opcode[] value() default {Opcode.TEXT, Opcode.BINARY};

}
