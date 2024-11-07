package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.transformers.TransformerPerformer;
import de.craftsblock.craftsnet.api.utils.SessionStorage;
import de.craftsblock.craftsnet.events.requests.PostRequestEvent;
import de.craftsblock.craftsnet.events.requests.PreRequestEvent;
import de.craftsblock.craftsnet.events.requests.routes.RouteRequestEvent;
import de.craftsblock.craftsnet.events.requests.shares.ShareFileLoadedEvent;
import de.craftsblock.craftsnet.events.requests.shares.ShareRequestEvent;
import de.craftsblock.craftsnet.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles HTTP requests and routes them to the appropriate handlers based on the registered routes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.4
 * @see WebServer
 * @since 3.0.1-SNAPSHOT
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
     * @param httpExchange The HTTP exchange object representing the incoming request and outgoing response.
     * @throws IOException If an I/O error occurs during request processing.
     */
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            // Extract relevant information from the incoming request.
            String requestMethod = httpExchange.getRequestMethod();
            HttpMethod httpMethod = HttpMethod.parse(requestMethod);

            String url = httpExchange.getRequestURI().toString();
            Headers headers = httpExchange.getRequestHeaders();

            Response response = new Response(this.craftsNet, httpExchange, httpMethod);
            try {
                String ip;
                if (headers.containsKey("Cf-connecting-ip")) ip = headers.getFirst("Cf-connecting-ip");
                else if (headers.containsKey("X-forwarded-for")) ip = headers.getFirst("X-forwarded-for").split(", ")[0];
                else ip = httpExchange.getRemoteAddress().getAddress().getHostAddress();

                String domain;
                if (headers.containsKey("X-Forwarded-Host")) domain = headers.getFirst("X-forwarded-Host").split(":")[0];
                else domain = headers.getFirst("Host").split(":")[0];

                // Create a Request object to encapsulate the incoming request information.
                try (Request request = new Request(this.craftsNet, httpExchange, headers, url, ip, domain, httpMethod)) {
                    Exchange exchange = new Exchange(url, request, response, new SessionStorage());

                    PreRequestEvent event = new PreRequestEvent(exchange);
                    craftsNet.listenerRegistry().call(event);
                    if (event.isCancelled()) return;

                    Map.Entry<Boolean, Boolean> result = handle(exchange);
                    craftsNet.listenerRegistry().call(new PostRequestEvent(exchange, result.getKey(), result.getValue()));
                }
            } catch (Throwable t) {
                if (craftsNet.fileLogger() != null) {
                    long errorID = craftsNet.fileLogger().createErrorLog(this.craftsNet, t, "http", url);
                    logger.error(t, "Error: " + errorID);
                    if (response.headersSent()) response.setCode(500);
                    if (!httpMethod.equals(HttpMethod.HEAD) && !httpMethod.equals(HttpMethod.UNKNOWN))
                        response.print(Json.empty()
                                .set("error.message", "An unexpected exception happened whilst processing your request!")
                                .set("error.identifier", errorID));
                } else logger.error(t);
            } finally {
                response.close();
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * Handles a http request and determines whether a valid route or share is available
     * for the given exchange. If a matching route or share is found, the appropriate handler
     * is invoked. Otherwise, an error is returned indicating the path is not found.
     *
     * @param exchange The {@link Exchange} containing the request and response.
     * @return A {@link Map.Entry} where the first {@link Boolean} indicates if a route or share
     * was found (true if found, false otherwise), and the second {@link Boolean} indicates
     * if it was a shared file (true if so, false otherwise).
     * @throws Exception If any error occurs during the handling of the exchange.
     */
    private Map.Entry<Boolean, Boolean> handle(Exchange exchange) throws Exception {
        Request request = exchange.request();
        Response response = exchange.response();

        String url = request.getUrl();
        HttpMethod httpMethod = request.getHttpMethod();

        // Check if the route is registered and process it if so
        if (handleRoute(exchange)) return Map.entry(true, false);

        // Check if the URL can be handled as a shared resource and if it accepts the current http method
        if (registry.isShare(url) && registry.canShareAccept(url, httpMethod)) {
            handleShare(exchange);
            return Map.entry(true, true);
        }

        // If no matching route or share is found, respond with an error message and log the failed request
        response.setCode(404);
        respondWithError(response, "Path do not match any API endpoint!");
        logger.info(httpMethod.toString() + " " + url + " from " + request.getIp() + " \u001b[38;5;9m[NOT FOUND]");
        return Map.entry(false, false);
    }

    /**
     * Handles route-specific requests by delegating to the appropriate route handler.
     *
     * @param exchange The {@link Exchange} representing the request.
     * @throws IOException               If an I/O error occurs during request processing.
     * @throws InvocationTargetException If an error occurs while invoking the route handler.
     * @throws IllegalAccessException    If the route handler cannot be accessed.
     */
    private boolean handleRoute(Exchange exchange) throws Exception {
        Request request = exchange.request();
        Response response = exchange.response();

        HttpMethod requestMethod = request.getHttpMethod();
        String url = request.getUrl();
        String ip = request.getIp();

        // Find the registered route mapping based on the request.
        EnumMap<ProcessPriority.Priority, List<RouteRegistry.EndpointMapping>> routes = registry.getRoute(request);

        // If no matching route is found abort with return false
        if (routes == null || routes.isEmpty()) return false;

        // Associate the matched route with the Request object.
        request.setRoutes(routes.values().stream().flatMap(Collection::stream).toList());

        // Create a RequestEvent and call listeners before invoking the API handler method.
        RouteRequestEvent event = new RouteRequestEvent(exchange);
        craftsNet.listenerRegistry().call(event);
        if (event.isCancelled()) {
            String cancelReason = event.hasCancelReason() ? event.getCancelReason() : "ABORTED";
            logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[" + cancelReason + "]");
            return true;
        }
        logger.info(requestMethod + " " + url + " from " + ip);

        Pattern validator = routes.get(routes.keySet().stream().findFirst().orElseThrow()).get(0).validator();
        Matcher matcher = validator.matcher(url);
        if (!matcher.matches()) {
            respondWithError(response, "There was an unexpected error while matching!");
            return true;
        }

        // Prepare the argument array to be passed to the API handler method.
        Object[] args = new Object[matcher.groupCount()];

        args[0] = exchange;
        for (int i = 2; i <= matcher.groupCount(); i++)
            args[i - 1] = matcher.group(i);

        // Create a transformer performer which handles all transformers
        TransformerPerformer transformerPerformer = new TransformerPerformer(this.craftsNet, validator, 1, e -> {
            response.println(Json.empty().set("error", "Could not process transformer: " + e.getMessage()).toString());
        });

        // Loop through all priorities
        for (ProcessPriority.Priority priority : routes.keySet())
            for (RouteRegistry.EndpointMapping mapping : routes.get(priority)) {
                if (!(mapping.handler() instanceof RequestHandler handler)) continue;
                Method method = mapping.method();

                // Perform all transformers and continue if passingArgs is null
                Object[] passingArgs = transformerPerformer.perform(mapping.handler(), method, args);
                if (passingArgs == null)
                    continue;

                // Call the method of the route handler
                method.setAccessible(true);
                method.invoke(handler, passingArgs);
                method.setAccessible(false);
            }

        // Clean up to free up memory
        if (args.length == 1 && args[0] instanceof Exchange e) e.storage().clear();
        transformerPerformer.clearCache();

        return true;
    }

    /**
     * Handles share-specific requests by delegating to the appropriate share handler.
     *
     * @param exchange The {@link Exchange} representing the request.
     * @throws IOException               If an I/O error occurs during request processing.
     * @throws InvocationTargetException If an error occurs while invoking the share handler.
     * @throws IllegalAccessException    If the share handler cannot be accessed.
     */
    private void handleShare(Exchange exchange) throws InvocationTargetException, IllegalAccessException, IOException {
        Request request = exchange.request();
        Response response = exchange.response();

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
        ShareRequestEvent event = new ShareRequestEvent(url, path, exchange, registry.getShare(url));
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
            response.print(DefaultPages.notallowed(domain, request.unsafe().getLocalAddress().getPort()));
            return;
        } else if (!share.exists()) {
            response.setCode(404);
            response.setContentType("text/html; charset=utf-8");
            response.print(DefaultPages.notfound(domain, request.unsafe().getLocalAddress().getPort()));
            return;
        }

        response.setContentType(fileLoadedEvent.getContentType(), "text/plain");

        response.print(share);
        event.getExchange().storage().clear(); // Clear the session storage as it is no longer needed
    }

    /**
     * Responds to the client with an error message.
     *
     * @param response The Response object for managing the outgoing response.
     * @param message  The error message to be included in the response.
     * @throws IOException If an I/O error occurs during response handling.
     */
    private static void respondWithError(Response response, String message) throws IOException {
        response.print(Json.empty().set("success", false).set("message", message));
    }

}
