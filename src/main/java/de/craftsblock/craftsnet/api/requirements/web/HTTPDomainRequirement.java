package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.Domain;
import de.craftsblock.craftsnet.api.http.Request;

import java.util.List;

/**
 * A specific web requirement that checks if the domain of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see WebRequirement
 * @since 3.0.5-SNAPSHOT
 */
public class HTTPDomainRequirement extends WebRequirement {

    /**
     * Constructs a new HTTP domain requirement.
     */
    public HTTPDomainRequirement() {
        super(Domain.class);
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
        String domain = request.getDomain();
        if (domain == null) return false;

        return requirements.contains("*") || requirements.contains(domain);
    }

}
