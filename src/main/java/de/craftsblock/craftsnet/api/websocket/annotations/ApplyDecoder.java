package de.craftsblock.craftsnet.api.websocket.annotations;

import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeDecoder;

import java.lang.annotation.*;

/**
 * Specifies a {@link WebSocketSafeTypeDecoder} to be applied to the second parameter
 * of a WebSocket handler method.
 * <p>
 * This annotation allows developers to automatically decode an incoming WebSocket
 * {@link Frame} into a specific Java type before the method is invoked.
 * <p>
 * The annotated method must have at least two parameters. The decoder will be used to process
 * the second parameter from the {@link Frame}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see WebSocketSafeTypeDecoder
 * @since 3.5.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplyDecoder {

    /**
     * The {@link WebSocketSafeTypeDecoder} class used to decode the second parameter.
     *
     * @return the decoder class
     */
    Class<? extends WebSocketSafeTypeDecoder<?>> value();

}
