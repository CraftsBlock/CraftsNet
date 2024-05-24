package de.craftsblock.craftsnet.api.requirements.websocket;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.Domain;
import de.craftsblock.craftsnet.api.requirements.web.WebRequirement;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;

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
public class WSDomainRequirement extends WebSocketRequirement {

    /**
     * Constructs a new websocket domain requirement.
     */
    public WSDomainRequirement() {
        super(Domain.class);
    }

    /**
     * Checks if the requirement applies given the specified websocket client and socket mapping.
     *
     * @param client        the websocket client
     * @param socketMapping the socket mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(WebSocketClient client, RouteRegistry.EndpointMapping socketMapping) {
        List<String> rawRequirements = socketMapping.getRequirements(getAnnotation(), String.class);
        if (rawRequirements == null) return true;
        List<String> requirements = new ArrayList<>(rawRequirements);

        String domain = client.getDomain();
        if (domain == null) return false;

        return requirements.contains("*") || requirements.contains(domain);
    }

}
