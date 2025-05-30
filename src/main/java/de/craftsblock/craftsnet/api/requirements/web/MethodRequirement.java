package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequestMethod;

import java.util.Arrays;
import java.util.List;

/**
 * A specific web requirement that checks if the request method of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
 * @see WebRequirement
 * @since 3.0.5-SNAPSHOT
 */
public class MethodRequirement extends WebRequirement {

    /**
     * Constructs a new method requirement.
     */
    public MethodRequirement() {
        super(RequestMethod.class);
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

        List<HttpMethod> requirements = endpointMapping.getRequirements(getAnnotation(), "value");

        HttpMethod method = request.getHttpMethod();
        if (requirements.contains(HttpMethod.ALL))
            return Arrays.stream(HttpMethod.ALL.getMethods())
                    .anyMatch(httpMethod -> method.equals(httpMethod) || requirements.contains(httpMethod));

        return requirements.contains(method);
    }

}
