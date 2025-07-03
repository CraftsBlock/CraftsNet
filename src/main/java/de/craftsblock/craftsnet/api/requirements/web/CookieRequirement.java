package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequireCookie;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A specific web requirement that checks if the cookie list of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
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
        if (!endpointMapping.isPresent(getAnnotation(), "value")) return true;

        List<String> requirements = endpointMapping.getRequirements(getAnnotation(), "value");
        if (requirements == null) return true;

        Set<String> cookies = request.getCookies().keySet();
        if (cookies.isEmpty()) return false;

        return new HashSet<>(cookies).containsAll(requirements);
    }

}
