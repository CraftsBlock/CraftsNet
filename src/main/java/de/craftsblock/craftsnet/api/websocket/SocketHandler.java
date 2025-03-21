package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.websocket.annotations.Socket;

/**
 * Represents a contract for classes that handle WebSocket connections in a server application.
 * WebSocket server handlers must implement this interface to define their behavior.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see Socket
 * @since 2.1.1-SNAPSHOT
 */
public interface SocketHandler extends Handler {

    // This interface does not define any specific methods, but it serves as a marker interface for WebSocket server handlers.
    // Classes that implement this interface are expected to handle WebSocket connections and related events.
    // Any specific methods and behavior required for handling WebSocket connections should be defined in classes
    // that implement this interface.

}
