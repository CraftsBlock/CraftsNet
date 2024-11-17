package de.craftsblock.craftsnet.api.websocket.annotations;

import java.lang.annotation.*;

/**
 * Custom annotation used to mark methods as message receivers in WebSocket handler classes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see Socket
 * @since CraftsNet-2.1.1
 * @deprecated Will not do anything in the future, it was replaced by the {@link Socket} annotation,
 * which now works like the {@link de.craftsblock.craftsnet.api.http.annotations.Route} annotation. All
 * developers are recommended to switch to the new websocket registration mechanism, as this mechanism
 * is only available up to release 4.0.0 of CraftsNet.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated(since = "3.0.3", forRemoval = true)
public @interface MessageReceiver {

    // This annotation does not define any elements as it only serves as a marker annotation.
    // It is used to indicate that the annotated method should be invoked when the WebSocket
    // server receives messages for the associated WebSocket path.
    // Since it does not have any elements, the presence of this annotation on a method is sufficient
    // to indicate its role as a message receiver.

}
