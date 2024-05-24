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
 * @version 1.0.0
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
     * @param request       the HTTP request
     * @param routeMapping  the route mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(Request request, RouteRegistry.RouteMapping routeMapping) {
        Headers headers = request.getHeaders();
        List<String> requirements = routeMapping.getRequirements(getAnnotation(), String.class);
        if (requirements == null || requirements.isEmpty()) return true;
        if (headers == null) return false;

        if (!headers.isEmpty()) return headers.keySet().containsAll(requirements);
        return false;
    }

}
