package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequireParameter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A specific web requirement that checks if the url parameter list of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see WebRequirement
 * @since 3.0.6-SNAPSHOT
 */
public class QueryParameterRequirement extends WebRequirement {

    /**
     * Constructs a new query parameter requirement.
     */
    public QueryParameterRequirement() {
        super(RequireParameter.class);
    }

    /**
     * Checks if the requirement applies given the specified request and endpoint mapping.
     *
     * @param request         the HTTP request
     * @param endpointMapping the endpoint mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(Request request, RouteRegistry.EndpointMapping endpointMapping) {
        if (!endpointMapping.isPresent(getAnnotation(), "value")) return true;

        List<String> requirements = endpointMapping.getRequirements(getAnnotation(), "value");
        if (request.getQueryParams().isEmpty()) return false;

        Set<String> parameters = request.getQueryParams().keySet();
        return new HashSet<>(requirements).containsAll(parameters);
    }

}
