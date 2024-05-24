package de.craftsblock.craftsnet.api.requirements.web;

import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.annotations.RequestMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A specific web requirement that checks if the request method of an HTTP request matches certain criteria.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
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
     * @param request      the HTTP request
     * @param routeMapping the route mapping
     * @return {@code true} if the requirement applies, {@code false} otherwise
     */
    @Override
    public boolean applies(Request request, RouteRegistry.EndpointMapping routeMapping) {
        List<HttpMethod> rawRequirements = routeMapping.getRequirements(getAnnotation(), HttpMethod.class);
        if (rawRequirements == null) return true;
        List<HttpMethod> requirements = new ArrayList<>(rawRequirements);

        HttpMethod method = request.getHttpMethod();
        if (requirements.contains(HttpMethod.ALL)) {
            requirements.remove(HttpMethod.ALL);
            Arrays.stream(HttpMethod.ALL.getMethods()).forEach(s -> requirements.add(HttpMethod.parse(s)));
            removeDuplicates(requirements);
        }
        return requirements.contains(method);
    }

    /**
     * Removes duplicates from a list by converting it to a set and then back to a list.
     *
     * @param <T>  The type of elements in the list.
     * @param list The list from which duplicates will be removed.
     */
    private <T> void removeDuplicates(List<T> list) {
        HashSet<T> unique = new HashSet<>(list);
        list.clear();
        list.addAll(unique);
    }

}
