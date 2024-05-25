package de.craftsblock.craftsnet.api.requirements.websocket;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.requirements.web.WebRequirement;
import de.craftsblock.craftsnet.api.websocket.ControlByte;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.annotations.RequireMessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * A specific websocket requirement that checks if the domain of a websocket connection matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see WebRequirement
 * @since 3.0.5-SNAPSHOT
 */
public class MessageTypeRequirement extends WebSocketRequirement<WebSocketClient.Message> {

    /**
     * Constructs a new websocket message type requirement.
     */
    public MessageTypeRequirement() {
        super(RequireMessageType.class);
    }

    /**
     * Checks if the requirement applies given the specified websocket message and socket mapping.
     *
     * @param message         the websocket message
     * @param endpointMapping the socket mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(WebSocketClient.Message message, RouteRegistry.EndpointMapping endpointMapping) {
        List<ControlByte> rawRequirements = endpointMapping.getRequirements(getAnnotation(), ControlByte.class);
        if (rawRequirements == null) return true;
        List<ControlByte> requirements = new ArrayList<>(rawRequirements);

        return requirements.contains(message.controlByte());
    }

}
