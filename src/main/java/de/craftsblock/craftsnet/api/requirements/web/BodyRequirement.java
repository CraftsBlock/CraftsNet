package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequireBody;
import de.craftsblock.craftsnet.api.http.body.Body;

/**
 * A specific web requirement that checks if the body of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see WebRequirement
 * @since 3.0.5-SNAPSHOT
 */
public class BodyRequirement extends WebRequirement {

    /**
     * Constructs a new body requirement.
     */
    public BodyRequirement() {
        super(RequireBody.class);
    }

    /**
     * Checks if the requirement applies given the specified request and route mapping.
     *
     * @param request      the HTTP request
     * @param routeMapping the route mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(Request request, RouteRegistry.EndpointMapping routeMapping) {
        if (!routeMapping.requirements().containsKey(getAnnotation()) || routeMapping.requirements().get(getAnnotation()).isEmpty()) return true;
        if (!request.hasBody()) return false;

        return routeMapping.getRequirements(RequireBody.class, Class.class).parallelStream()
                .filter(Body.class::isAssignableFrom)
                .allMatch(type -> request.getBody().isBodyFromType(type));
    }

}
