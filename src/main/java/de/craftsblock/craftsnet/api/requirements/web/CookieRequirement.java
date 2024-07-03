package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequireCookie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A specific web requirement that checks if the cookie list of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see WebRequirement
 * @since 3.0.6-SNAPSHOT
 */
public class CookieRequirement extends WebRequirement {

    /**
     * Constructs a new cookie requirement.
     */
    public CookieRequirement() {
        super(RequireCookie.class);
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
        List<String> rawRequirements = endpointMapping.getRequirements(getAnnotation(), String.class);
        if (rawRequirements == null) return true;
        List<String> requirements = new ArrayList<>(rawRequirements);

        if (request.getCookies().isEmpty()) return false;

        Set<String> cookies = request.getCookies().keySet();
        return new HashSet<>(requirements).containsAll(cookies);
    }

}
