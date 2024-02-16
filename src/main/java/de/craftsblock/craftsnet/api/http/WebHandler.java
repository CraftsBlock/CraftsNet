package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.events.RequestEvent;
import de.craftsblock.craftsnet.events.shares.ShareFileLoadedEvent;
import de.craftsblock.craftsnet.events.shares.ShareRequestEvent;
import de.craftsblock.craftsnet.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles HTTP requests and routes them to the appropriate handlers based on the registered routes.
 *
 * @author CraftsBlock
 * @version 1.0.0
 * @see WebServer
 * @since 3.0.1
 */
public class WebHandler implements HttpHandler {

    private static final Logger logger = CraftsNet.logger();
    private static final RouteRegistry registry = CraftsNet.routeRegistry();

    /**
     * Handles incoming HTTP requests and delegates them to the appropriate handlers.
     *
     * @param exchange The HTTP exchange object representing the incoming request and outgoing response.
     * @throws IOException If an I/O error occurs during request processing.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (Response response = new Response(exchange)) {
            // Extract relevant information from the incoming request.
            String domain = exchange.getRequestHeaders().getFirst("Host").split(":")[0];
            String requestMethod = exchange.getRequestMethod();
            String url = exchange.getRequestURI().toString();
            Headers headers = exchange.getRequestHeaders();

            String ip;
            if (headers.containsKey("Cf-connecting-ip")) ip = headers.getFirst("Cf-connecting-ip");
            else if (headers.containsKey("X-forwarded-for")) ip = headers.getFirst("X-forwarded-for").split(", ")[0];
            else ip = exchange.getRemoteAddress().getAddress().getHostAddress();

            if (registry.isShare(url) && HttpMethod.parse(requestMethod) == HttpMethod.GET)
                handleShare(response, exchange, domain, requestMethod, url, ip);
            else
                handleRoute(response, exchange, domain, requestMethod, url, ip, headers);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error(e);
        }
    }

    /**
     * Handles route-specific requests by delegating to the appropriate route handler.
     *
     * @param response      The Response object for managing the outgoing response.
     * @param exchange      The HTTP exchange object representing the incoming request and outgoing response.
     * @param domain        The domain extracted from the request headers.
     * @param requestMethod The HTTP request method (e.g., GET, POST).
     * @param url           The requested URL.
     * @param ip            The IP address of the client making the request.
     * @param headers       The headers of the incoming request.
     * @throws IOException              If an I/O error occurs during request processing.
     * @throws InvocationTargetException If an error occurs while invoking the route handler.
     * @throws IllegalAccessException    If the route handler cannot be accessed.
     */
    private void handleRoute(Response response, HttpExchange exchange, String domain, String requestMethod, String url, String ip, Headers headers) throws IOException, InvocationTargetException, IllegalAccessException {
        // Extract relevant information from the incoming request.
        String[] stripped = url.split("\\?");
        String query = (stripped.length == 2 ? stripped[1] : "");

        // Create a Request object to encapsulate the incoming request information.
        Request request = new Request(exchange, query, ip);
        url = stripped[0];

        // Check if the requested URL contains "/favicon.ico". If so, return an error response.
        if (url.contains("favicon.ico")) {
            respondWithError(response, "No permission!");
            return;
        }

        // Find the registered route mapping based on the URL and request method.
        List<RouteRegistry.RouteMapping> routes = registry.getRoute(url, domain, headers.keySet(), requestMethod);

        // If no matching route is found, return an error response.
        if (routes == null || routes.isEmpty()) {
            respondWithError(response, "Path do not match any API endpoint!");
            logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[NOT FOUND]");
            return;
        }
        // Associate the matched route with the Request object.
        request.setRoutes(routes);

        // Create a RequestEvent and call listeners before invoking the API handler method.
        RequestEvent event = new RequestEvent(new Exchange(url, request, response), routes);
        CraftsNet.listenerRegistry().call(event);
        if (event.isCancelled()) {
            logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[ABORTED]");
            return;
        }
        logger.info(requestMethod + " " + url + " from " + ip);

        Pattern validator = routes.get(0).validator();
        Matcher matcher = validator.matcher(url);
        if (!matcher.matches()) {
            respondWithError(response, "There was an unexpected error while matching!");
            return;
        }

        // Prepare the argument array to be passed to the API handler method.
        Object[] args = new Object[matcher.groupCount()];
        args[0] = new Exchange(url, request, response);
        for (int i = 2; i <= matcher.groupCount(); i++)
            args[i - 1] = matcher.group(i);

        // Use a regular expression matcher to extract path parameters from the URL.
        ProcessPriority.Priority priority = ProcessPriority.Priority.LOWEST;
        while (priority != null) {
            if (routes.isEmpty()) break;

            Iterator<RouteRegistry.RouteMapping> iterator = routes.iterator();
            while (iterator.hasNext()) {
                RouteRegistry.RouteMapping mapping = iterator.next();
                if (!mapping.priority().equals(priority)) continue;
                iterator.remove();
                mapping.method().invoke(mapping.handler(), args);
            }

            priority = priority.next();
        }
    }

    /**
     * Handles share-specific requests by delegating to the appropriate share handler.
     *
     * @param response      The Response object for managing the outgoing response.
     * @param exchange      The HTTP exchange object representing the incoming request and outgoing response.
     * @param domain        The domain extracted from the request headers.
     * @param requestMethod The HTTP request method (e.g., GET, POST).
     * @param url           The requested URL.
     * @param ip            The IP address of the client making the request.
     * @throws IOException              If an I/O error occurs during request processing.
     * @throws InvocationTargetException If an error occurs while invoking the share handler.
     * @throws IllegalAccessException    If the share handler cannot be accessed.
     */
    private void handleShare(Response response, HttpExchange exchange, String domain, String requestMethod, String url, String ip) throws InvocationTargetException, IllegalAccessException, IOException {
        File folder = registry.getShareFolder(url);
        Matcher matcher = registry.getSharePattern(url).matcher(url);
        if (!matcher.matches()) {
            respondWithError(response, "There was an unexpected error while matching!");
            return;
        }

        String path = matcher.group(1);
        ShareRequestEvent event = new ShareRequestEvent(path);
        CraftsNet.listenerRegistry().call(event);
        for (String key : event.getHeaders().keySet())
            response.setHeader(key, event.getHeaders().getFirst(key));
        if (event.isCancelled()) {
            logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[SHARE ABORTED]");
            return;
        }
        path = event.getPath();
        logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;205m[SHARED]");

        File share = new File(folder, (path.isBlank() ? "index.html" : path));
        ShareFileLoadedEvent fileLoadedEvent = new ShareFileLoadedEvent(share);
        CraftsNet.listenerRegistry().call(fileLoadedEvent);
        share = fileLoadedEvent.getFile();

        assert share != null;
        if (!share.getCanonicalPath().startsWith(folder.getCanonicalPath() + File.separator)) {
            response.setCode(403);
            response.setContentType("text/html; charset=utf-8");
            response.print(DefaultPages.notallowed(domain, exchange.getLocalAddress().getPort()));
            return;
        } else if (!share.exists()) {
            response.setCode(404);
            response.setContentType("text/html; charset=utf-8");
            response.print(DefaultPages.notfound(domain, exchange.getLocalAddress().getPort()));
            return;
        }
        response.setContentType(fileLoadedEvent.getContentType());
        response.print(share);
    }

    /**
     * Responds to the client with an error message.
     *
     * @param response      The Response object for managing the outgoing response.
     * @param errorMessage  The error message to be included in the response.
     * @throws IOException If an I/O error occurs during response handling.
     */
    private static void respondWithError(Response response, String errorMessage) throws IOException {
        Json json = Json.empty();
        json.set("success", false);
        json.set("message", errorMessage);
        response.print(json.asString());
    }

}
