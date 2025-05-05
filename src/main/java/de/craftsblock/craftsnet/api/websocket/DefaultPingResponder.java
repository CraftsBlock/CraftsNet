package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.events.sockets.message.ReceivedPingMessageEvent;

/**
 * A default implementation of a ping responder for handling incoming ping messages.
 * This responder sends a pong message back to the client when a ping message is received.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see ReceivedPingMessageEvent
 * @see WebSocketClient
 * @since 3.0.5-SNAPSHOT
 */
public class DefaultPingResponder implements ListenerAdapter {

    private static final DefaultPingResponder PING_RESPONDER = new DefaultPingResponder();

    /**
     * Handles the received ping message event by sending a pong message back to the client.
     *
     * @param event The event containing the received ping message and the client it originated from.
     */
    @EventHandler
    public void handlePing(ReceivedPingMessageEvent event) {
        if (!event.getClient().isConnected()) return;
        event.getClient().sendPong(event.getData());
    }

    /**
     * Unregisters the DefaultPingResponder from the specified CraftsNet instance.
     *
     * @param craftsNet The CraftsNet instance from which to unregister the responder.
     */
    public static void unregister(CraftsNet craftsNet) {
        craftsNet.listenerRegistry().unregister(PING_RESPONDER);
    }

    /**
     * Registers the DefaultPingResponder with the specified CraftsNet instance.
     * If the responder is already registered, it is unregistered first before registering again.
     *
     * @param craftsNet The CraftsNet instance with which to register the responder.
     */
    public static void register(CraftsNet craftsNet) {
        unregister(craftsNet);
        craftsNet.listenerRegistry().register(PING_RESPONDER);
    }

}
