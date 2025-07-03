package de.craftsblock.craftsnet.api.requirements.web;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequireHeaders;

import java.util.List;

/**
 * A specific web requirement that checks if the headers of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
 * @see WebRequirement
 * @since 3.0.5-SNAPSHOT
 */
public class HeadersRequirement extends WebRequirement {

    /**
     * Constructs a new headers requirement.
     */
    public HeadersRequirement() {
        super(RequireHeaders.class);
    }

    /**
     * Checks if the requirement applies given the specified request and route mapping.
     *
     * @param request         the HTTP request
     * @param endpointMapping the route mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(Request request, RouteRegistry.EndpointMapping endpointMapping) {
        if (!endpointMapping.isPresent(getAnnotation(), "value")) return true;

        List<String> requirements = endpointMapping.getRequirements(getAnnotation(), "value");
        if (requirements == null) return true;

        Headers headers = request.getHeaders();
        if (headers == null || headers.isEmpty()) return false;

        return headers.keySet().containsAll(requirements);
    }

}
