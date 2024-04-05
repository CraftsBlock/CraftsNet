package de.craftsblock.craftsnet.api.websocket.annotations;

import de.craftsblock.craftsnet.api.websocket.SocketHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation used to mark classes as WebSocket handlers.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see MessageReceiver
 * @see SocketHandler
 * @since 2.1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Socket {

    /**
     * Specifies the WebSocket path to which this handler is associated.
     * This path is used to identify which WebSocket endpoint the handler is responsible for.
     *
     * @return The WebSocket path associated with this handler.
     */
    String value() default "/";


}
