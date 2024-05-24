package de.craftsblock.craftsnet.api.requirements.web;

import com.sun.net.httpserver.Headers;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequireContentType;

import java.util.HashSet;
import java.util.List;

/**
 * A specific web requirement that checks if the content-type of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see WebRequirement
 * @since 3.0.5-SNAPSHOT
 */
public class ContentTypeRequirement extends WebRequirement {

    /**
     * Constructs a new content type requirement.
     */
    public ContentTypeRequirement() {
        super(RequireContentType.class);
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
        if (headers == null || headers.isEmpty() || !headers.containsKey("content-type")) return false;

        List<String> contentTypes = headers.get("content-type");
        return contentTypes.contains("*") || new HashSet<>(contentTypes).containsAll(requirements);
    }

}
