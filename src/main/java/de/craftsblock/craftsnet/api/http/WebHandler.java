package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.transformers.TransformerPerformer;
import de.craftsblock.craftsnet.events.RequestEvent;
import de.craftsblock.craftsnet.events.shares.ShareFileLoadedEvent;
import de.craftsblock.craftsnet.events.shares.ShareRequestEvent;
import de.craftsblock.craftsnet.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles HTTP requests and routes them to the appropriate handlers based on the registered routes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.0
 * @see WebServer
 * @since CraftsNet-3.0.1
 */
public class WebHandler implements HttpHandler {

    private final CraftsNet craftsNet;
    private final Logger logger;
    private final RouteRegistry registry;

    /**
     * Constructs a new instance of the WebHandler
     *
     * @param craftsNet The CraftsNet instance which instantiates this web handler.
     */
    public WebHandler(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.logger();
        this.registry = this.craftsNet.routeRegistry();
    }

    /**
     * Handles incoming HTTP requests and delegates them to the appropriate handlers.
     *
     * @param exchange The HTTP exchange object representing the incoming request and outgoing response.
     * @throws IOException If an I/O error occurs during request processing.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (Response response = new Response(this.craftsNet, exchange)) {
            // Extract relevant information from the incoming request.
            String requestMethod = exchange.getRequestMethod();
            HttpMethod httpMethod = HttpMethod.parse(requestMethod);
            String url = exchange.getRequestURI().toString();
            Headers headers = exchange.getRequestHeaders();

            try {
                String ip;
                if (headers.containsKey("Cf-connecting-ip")) ip = headers.getFirst("Cf-connecting-ip");
                else if (headers.containsKey("X-forwarded-for")) ip = headers.getFirst("X-forwarded-for").split(", ")[0];
                else ip = exchange.getRemoteAddress().getAddress().getHostAddress();

                String domain;
                if (headers.containsKey("X-Forwarded-Host")) domain = headers.getFirst("X-forwarded-Host").split(":")[0];
                else domain = headers.getFirst("Host").split(":")[0];

                // Create a Request object to encapsulate the incoming request information.
                Request request = new Request(this.craftsNet, exchange, headers, url, ip, domain, httpMethod);

                if (handleRoute(response, request)) return;
                if (registry.isShare(url) && registry.canShareAccept(url, httpMethod)) {
                    handleShare(exchange, response, request);
                    return;
                }

                // Return an error as there is no route or share on the path
                respondWithError(response, "Path do not match any API endpoint!");
                logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[NOT FOUND]");
            } catch (Throwable t) {
                if (craftsNet.fileLogger() != null) {
                    long errorID = craftsNet.fileLogger().createErrorLog(this.craftsNet, t, "http", url);
                    logger.error(t, "Error: " + errorID);
                    response.println(Json.empty()
                            .set("error.message", "An unexpected exception happened whilst processing your request!")
                            .set("error.identifier", errorID)
                            .asString());
                }
            }
        }
    }

    /**
     * Handles route-specific requests by delegating to the appropriate route handler.
     *
     * @param response The Response object for managing the outgoing response.
     * @param request  The Request object for retrieving important information about the response.
     * @throws IOException               If an I/O error occurs during request processing.
     * @throws InvocationTargetException If an error occurs while invoking the route handler.
     * @throws IllegalAccessException    If the route handler cannot be accessed.
     */
    private boolean handleRoute(Response response, Request request) throws Exception {
        String domain = request.getDomain();
        HttpMethod requestMethod = request.getHttpMethod();
        String url = request.getUrl();
        String ip = request.getIp();
        Headers headers = request.getHeaders();

        // Find the registered route mapping based on the request.
        List<RouteRegistry.RouteMapping> routes = registry.getRoute(request);

        // If no matching route is found abort with return false
        if (routes == null || routes.isEmpty()) return false;

        // Associate the matched route with the Request object.
        request.setRoutes(routes);

        // Create a RequestEvent and call listeners before invoking the API handler method.
        RequestEvent event = new RequestEvent(new Exchange(url, request, response), routes);
        craftsNet.listenerRegistry().call(event);
        if (event.isCancelled()) {
            String cancelReason = event.hasCancelReason() ? event.getCancelReason() : "ABORTED";
            logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[" + cancelReason + "]");
            return true;
        }
        logger.info(requestMethod + " " + url + " from " + ip);

        Pattern validator = routes.get(0).validator();
        Matcher matcher = validator.matcher(url);
        if (!matcher.matches()) {
            respondWithError(response, "There was an unexpected error while matching!");
            return true;
        }

        // Prepare the argument array to be passed to the API handler method.
        Object[] args = new Object[matcher.groupCount()];

        args[0] = new Exchange(url, request, response);
        for (int i = 2; i <= matcher.groupCount(); i++)
            args[i - 1] = matcher.group(i);

        // Create a transformer performer which handles all transformers
        TransformerPerformer transformerPerformer = new TransformerPerformer(this.craftsNet, validator, 1, e -> {
            response.println(Json.empty().set("error", "Could not process transformer: " + e.getMessage()).asString());
        });

        // Loop through all priorities
        ProcessPriority.Priority priority = ProcessPriority.Priority.LOWEST;
        while (priority != null) {
            if (routes.isEmpty()) break;

            // Loop through all registered routes
            Iterator<RouteRegistry.RouteMapping> iterator = routes.iterator();
            while (iterator.hasNext()) {
                RouteRegistry.RouteMapping mapping = iterator.next();
                if (!mapping.priority().equals(priority)) continue;
                iterator.remove();

                RequestHandler handler = mapping.handler();
                Method method = mapping.method();

                // Perform all transformers and continue if passingArgs is null
                Object[] passingArgs = transformerPerformer.perform(method, args);
                if (passingArgs == null)
                    continue;

                // Call the method of the route handler
                method.invoke(handler, passingArgs);
            }

            // Update the current process priority
            priority = priority.next();
        }

        // Clear up transformer cache to free up memory
        transformerPerformer.clearCache();

        return true;
    }

    /**
     * Handles share-specific requests by delegating to the appropriate share handler.
     *
     * @param exchange The HTTP exchange object representing the incoming request and outgoing response.
     * @param response The Response object for managing the outgoing response.
     * @param request  The request object for getting important information about the request.
     * @throws IOException               If an I/O error occurs during request processing.
     * @throws InvocationTargetException If an error occurs while invoking the share handler.
     * @throws IllegalAccessException    If the share handler cannot be accessed.
     */
    private void handleShare(HttpExchange exchange, Response response, Request request) throws InvocationTargetException, IllegalAccessException, IOException {
        String ip = request.getIp();
        String url = request.getUrl();
        String domain = request.getDomain();
        HttpMethod httpMethod = request.getHttpMethod();

        File folder = registry.getShareFolder(url);
        Matcher matcher = registry.getSharePattern(url).matcher(url);
        if (!matcher.matches()) {
            respondWithError(response, "There was an unexpected error while matching!");
            return;
        }

        String path = matcher.group(1);
        ShareRequestEvent event = new ShareRequestEvent(url, path, new Exchange(request.getUrl(), request, response), registry.getShare(url));
        craftsNet.listenerRegistry().call(event);
        if (event.isCancelled()) {
            String cancelReason = event.hasCancelReason() ? event.getCancelReason() : "SHARE ABORTED";
            logger.info(httpMethod + " " + url + " from " + ip + " \u001b[38;5;9m[" + cancelReason + "]");
            return;
        }

        for (String key : event.getHeaders().keySet())
            event.getHeader(key).forEach(value -> response.addHeader(key, value));

        path = event.getFilePath();
        logger.info(httpMethod + " " + url + " from " + ip + " \u001b[38;5;205m[SHARED]");

        File share = new File(folder, (path.isBlank() ? "index.html" : path));
        ShareFileLoadedEvent fileLoadedEvent = new ShareFileLoadedEvent(share);
        craftsNet.listenerRegistry().call(fileLoadedEvent);
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

        response.setContentType(fileLoadedEvent.getContentType(), "text/plain");
        response.print(share);
    }

    /**
     * Responds to the client with an error message.
     *
     * @param response     The Response object for managing the outgoing response.
     * @param errorMessage The error message to be included in the response.
     * @throws IOException If an I/O error occurs during response handling.
     */
    private static void respondWithError(Response response, String errorMessage) throws IOException {
        Json json = Json.empty();
        json.set("success", false);
        json.set("message", errorMessage);
        response.print(json.asString());
    }

}
