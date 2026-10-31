package de.craftsblock.craftsnet.api.http.builtin;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.EndpointHandler;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.entities.ResponseEntity;
import de.craftsblock.craftsnet.api.routing.RouteRegistration;
import de.craftsblock.craftsnet.api.routing.Router;
import de.craftsblock.craftsnet.api.routing.filter.http.HttpMethodFilter;

/**
 * Represents a default route handler implementation.
 * This class provides a default route for handling GET requests.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.3-SNAPSHOT
 */
public class DefaultRoute implements EndpointHandler {

    private static final DefaultRoute instance;

    static {
        instance = new DefaultRoute();
    }

    private RouteRegistration registration;

    /**
     * Private constructor to prevent external instantiation.
     *
     * @since 3.3.3-SNAPSHOT
     */
    private DefaultRoute() {
    }

    /**
     * Handles the default GET request.
     *
     * @param httpExchange The httpExchange object representing the HTTP request and response.
     */
    public ResponseEntity handleDefault(HttpExchange httpExchange) {
        return ResponseEntity.simple("Your running on CraftsNet v" + CraftsNet.version)
                .transform(response -> response.setContentType("text/text"));
    }

    public static void register(Router router) {
        unregister();
        instance.registration = router.http(
                "/",
                builder -> builder
                        .appendFilter(HttpMethodFilter.get())
                        .handle(instance::handleDefault)
        );
    }

    public static void unregister() {
        if (instance.registration != null) {
            instance.registration.unregister();
        }
    }

}
