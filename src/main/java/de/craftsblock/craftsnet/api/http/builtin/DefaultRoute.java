package de.craftsblock.craftsnet.api.http.builtin;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.http.RequestHandler;
import de.craftsblock.craftsnet.api.http.Response;
import de.craftsblock.craftsnet.api.http.annotations.RequestMethod;
import de.craftsblock.craftsnet.api.http.annotations.Route;

import java.io.IOException;

/**
 * Represents a default route handler implementation.
 * This class provides a default route for handling GET requests.
 */
public class DefaultRoute implements RequestHandler {

    /**
     * Handles the default GET request.
     *
     * @param exchange The exchange object representing the HTTP request and response.
     * @throws IOException If an I/O error occurs while handling the request.
     */
    @Route
    @RequestMethod(HttpMethod.ALL)
    public void handleDefault(Exchange exchange) throws IOException {
        Response response = exchange.response();
        response.setContentType("text/text");
        response.println("Your running on " + CraftsNet.version);
        response.println("If you can see this message CraftsNet is up and running!");
        response.println("This route has been registered to provide you whit an convenient way of check if CraftsNet's core is working. This route will disappear if you register own routes!");
    }

}
