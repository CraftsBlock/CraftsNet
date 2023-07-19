package de.craftsblock.craftsnet.api.websocket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation used to mark methods as message receivers in WebSocket handler classes.
 *
 * @author CraftsBlock
 * @see Socket
 * @since 2.1.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageReceiver {

    // This annotation does not define any elements as it only serves as a marker annotation.
    // It is used to indicate that the annotated method should be invoked when the WebSocket
    // server receives messages for the associated WebSocket path.
    // Since it does not have any elements, the presence of this annotation on a method is sufficient
    // to indicate its role as a message receiver.

}
