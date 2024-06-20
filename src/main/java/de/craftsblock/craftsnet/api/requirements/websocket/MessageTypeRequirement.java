package de.craftsblock.craftsnet.api.requirements.websocket;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.requirements.web.WebRequirement;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.api.websocket.Frame;
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
public class MessageTypeRequirement extends WebSocketRequirement<Frame> {

    /**
     * Constructs a new websocket message type requirement.
     */
    public MessageTypeRequirement() {
        super(RequireMessageType.class);
    }

    /**
     * Checks if the requirement applies given the specified websocket message and socket mapping.
     *
     * @param frame           the websocket message frame
     * @param endpointMapping the socket mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(Frame frame, RouteRegistry.EndpointMapping endpointMapping) {
        List<Opcode> rawRequirements = endpointMapping.getRequirements(getAnnotation(), Opcode.class);
        if (rawRequirements == null) return true;
        List<Opcode> requirements = new ArrayList<>(rawRequirements);

        return requirements.contains(frame.getOpcode());
    }

}
