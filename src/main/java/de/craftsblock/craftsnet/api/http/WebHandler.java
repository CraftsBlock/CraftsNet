package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.http.encoding.AcceptEncodingHelper;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoderRegistry;
import de.craftsblock.craftsnet.api.http.encoding.builtin.IdentityStreamEncoder;
import de.craftsblock.craftsnet.api.middlewares.Middleware;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareCallbackInfo;
import de.craftsblock.craftsnet.api.session.Session;
import de.craftsblock.craftsnet.api.session.SessionInfo;
import de.craftsblock.craftsnet.api.utils.Context;
import de.craftsblock.craftsnet.api.transformers.TransformerPerformer;
import de.craftsblock.craftsnet.api.utils.ProtocolVersion;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.events.requests.PostRequestEvent;
import de.craftsblock.craftsnet.events.requests.PreRequestEvent;
import de.craftsblock.craftsnet.events.requests.routes.RouteRequestEvent;
import de.craftsblock.craftsnet.events.requests.shares.ShareFileLoadedEvent;
import de.craftsblock.craftsnet.events.requests.shares.ShareRequestEvent;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles HTTP requests and routes them to the appropriate handlers based on the registered routes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.7.1
 * @see WebServer
 * @since 3.0.1-SNAPSHOT
 */
public class WebHandler implements HttpHandler {

    private static final String MESSAGE_FORMAT_REQUEST = "%s %s from %s";
    private static final String MESSAGE_FORMAT_REQUEST_ERROR = MESSAGE_FORMAT_REQUEST + " \u001b[38;5;9m[%s]";

    private final CraftsNet craftsNet;
    private final Logger logger;
    private final RouteRegistry registry;

    private final Scheme scheme;

    /**
     * Constructs a new instance of the WebHandler
     *
     * @param craftsNet The CraftsNet instance which instantiates this web handler.
     */
    public WebHandler(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.getLogger();
        this.registry = this.craftsNet.getRouteRegistry();
        this.scheme = Scheme.HTTP.getSsl(Scheme.HTTP.getServer(craftsNet).isSSL());
    }

    /**
     * Handles incoming HTTP requests and delegates them to the appropriate handlers.
     *
     * @param httpExchange The HTTP exchange object representing the incoming request and outgoing response.
     * @throws IOException If an I/O error occurs during request processing.
     */
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try (httpExchange) {
            // Extract relevant information from the incoming request.
            String requestMethod = httpExchange.getRequestMethod();
            HttpMethod httpMethod = HttpMethod.parse(requestMethod);

            String url = httpExchange.getRequestURI().toString().replaceAll("//+", "/");
            Headers headers = httpExchange.getRequestHeaders();

            StreamEncoderRegistry streamEncoderRegistry = craftsNet.getStreamEncoderRegistry();
            AtomicReference<StreamEncoder> streamEncoder = new AtomicReference<>(streamEncoderRegistry.retrieveEncoder(IdentityStreamEncoder.class));
            if (craftsNet.getBuilder().responseEncodingAllowed() && headers.containsKey("Accept-Encoding")) {
                var requestedEncodings = AcceptEncodingHelper.parseHeader(headers.getFirst("Accept-Encoding"));

                for (String requested : requestedEncodings)
                    if (streamEncoderRegistry.isAvailable(requested)) {
                        streamEncoder.set(streamEncoderRegistry.retrieveEncoder(requested));
                        break;
                    }
            }

            ProtocolVersion protocolVersion = ProtocolVersion.parse(this.scheme, httpExchange.getProtocol().split("/")[1]);
            Response response = new Response(this.craftsNet, streamEncoder.get(), httpExchange, httpMethod);
            try {
                String ip;
                if (headers.containsKey("Cf-connecting-ip")) ip = headers.getFirst("Cf-connecting-ip");
                else if (headers.containsKey("X-forwarded-for")) ip = headers.getFirst("X-forwarded-for").split(", ")[0];
                else ip = httpExchange.getRemoteAddress().getAddress().getHostAddress();

                String domain;
                if (headers.containsKey("X-Forwarded-Host")) domain = headers.getFirst("X-forwarded-Host").split(":")[0];
                else domain = headers.getFirst("Host").split(":")[0];

                // Create a Request object to encapsulate the incoming request information.
                try (Request request = new Request(this.craftsNet, httpExchange, headers, url, ip, domain, httpMethod);
                     Session session = craftsNet.getSessionCache().getOrNew(SessionInfo.extractSession(request));
                     Exchange exchange = new Exchange(new Context(), protocolVersion, request, response, session)) {
                    exchange.session().setExchange(exchange);

                    PreRequestEvent event = new PreRequestEvent(exchange);
                    craftsNet.getListenerRegistry().call(event);
                    if (event.isCancelled()) return;

                    Map.Entry<Boolean, Boolean> result = handle(exchange);
                    craftsNet.getListenerRegistry().call(new PostRequestEvent(exchange, result.getKey(), result.getValue()));
                }
            } catch (Throwable t) {
                if (craftsNet.getLogStream() != null) {
                    long errorID = craftsNet.getLogStream().createErrorLog(this.craftsNet, t, this.scheme.getName(), url);
                    logger.error("Error: %s", t, errorID);
                    if (!response.headersSent()) response.setCode(500);
                    if (!httpMethod.equals(HttpMethod.HEAD) && !httpMethod.equals(HttpMethod.UNKNOWN))
                        response.print(Json.empty()
                                .set("status", "500")
                                .set("message", "An unexpected exception happened whilst processing your request!")
                                .set("incident", errorID));
                } else logger.error(t);
            } finally {
                response.close();
            }
        } catch (Throwable t) {
            logger.error(t);
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

        // Handle global middlewares
        MiddlewareCallbackInfo callback = new MiddlewareCallbackInfo();
        for (Middleware middleware : craftsNet.getMiddlewareRegistry().getMiddlewares(exchange))
            if (middleware.isApplicable(exchange))
                middleware.handle(callback, exchange);

        // Cancel if the middleware callback is cancelled
        if (callback.isCancelled())
            return Map.entry(false, false);

        // Check if the route is registered and process it if so
        if (handleRoute(exchange)) return Map.entry(true, false);

        // Check if the URL can be handled as a shared resource and if it accepts the current http method
        if (registry.isShare(url) && registry.canShareAccept(url, httpMethod)) {
            handleShare(exchange);
            return Map.entry(true, true);
        }

        // If no matching route or share is found, respond with an error message and log the failed request
        respondWithError(response, 404, "Path do not match any API endpoint!");
        logger.info(MESSAGE_FORMAT_REQUEST_ERROR, httpMethod.toString(), url, request.getIp(), "NOT FOUND");
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
        craftsNet.getListenerRegistry().call(event);
        if (event.isCancelled()) {
            String cancelReason = event.hasCancelReason() ? event.getCancelReason() : "ABORTED";
            logger.info(MESSAGE_FORMAT_REQUEST_ERROR, requestMethod, url, ip, cancelReason);
            return true;
        }
        logger.info(MESSAGE_FORMAT_REQUEST, requestMethod, url, ip);

        // Create a transformer performer which handles all transformers
        TransformerPerformer transformerPerformer = new TransformerPerformer(this.craftsNet, 1, e -> {
            response.print(Json.empty().set("error", "Could not process transformer: " + e.getMessage()));
        });

        Map<String, Matcher> matchers = new HashMap<>();

        // Loop through all priorities
        for (ProcessPriority.Priority priority : routes.keySet())
            for (RouteRegistry.EndpointMapping mapping : routes.get(priority)) {
                if (!(mapping.handler() instanceof RequestHandler handler)) continue;
                Method method = mapping.method();

                Pattern validator = mapping.validator();
                Matcher matcher = matchers.computeIfAbsent(mapping.validator().pattern(), pattern -> {
                    Matcher fresh = validator.matcher(url);

                    if (!fresh.matches()) {
                        respondWithError(response, 500, "There was an unexpected error while matching!");
                    }

                    return fresh;
                });
                transformerPerformer.setValidator(validator);

                // Prepare the argument array to be passed to the API handler method.
                Object[] args = new Object[matcher.groupCount()];

                args[0] = exchange;
                for (int i = 2; i <= matcher.groupCount(); i++)
                    args[i - 1] = matcher.group(i);

                MiddlewareCallbackInfo callback = new MiddlewareCallbackInfo();
                mapping.middlewares().forEach(middleware -> middleware.handle(callback, exchange));
                if (callback.isCancelled()) continue;

                // Perform all transformers and continue if
                if (!transformerPerformer.perform(mapping.handler(), method, args))
                    continue;

                // Call the method of the route handler
                Object result = ReflectionUtils.invokeMethod(handler, method, args);
                if (result != null) exchange.response().print(result);
            }

        // Clean up to free up memory
        transformerPerformer.clearCache();
        matchers.clear();

        return true;
    }

    /**
     * Handles share-specific requests by delegating to the appropriate share handler.
     *
     * @param exchange The {@link Exchange} representing the request.
     */
    private void handleShare(Exchange exchange) {
        Request request = exchange.request();
        Response response = exchange.response();

        String ip = request.getIp();
        String url = request.getUrl();
        String domain = request.getDomain();
        HttpMethod httpMethod = request.getHttpMethod();

        Path folder = registry.getShareFolder(url).toAbsolutePath();
        Matcher matcher = registry.getSharePattern(url).matcher(url);
        if (!matcher.matches()) {
            respondWithError(response, 500, "There was an unexpected error while matching!");
            return;
        }

        ShareRequestEvent event = new ShareRequestEvent(url, matcher.group(1), exchange, registry.getShare(url));
        craftsNet.getListenerRegistry().call(event);
        if (event.isCancelled()) {
            String cancelReason = event.hasCancelReason() ? event.getCancelReason() : "SHARE ABORTED";
            logger.info(MESSAGE_FORMAT_REQUEST_ERROR, httpMethod, url, ip, cancelReason);
            return;
        }

        String path = event.getFilePath();
        logger.info(MESSAGE_FORMAT_REQUEST + " \u001b[38;5;205m[SHARED]", httpMethod, url, ip);

        ShareFileLoadedEvent fileLoadedEvent = new ShareFileLoadedEvent(exchange, folder.resolve((path.isBlank() ? "index.html" : path)));
        craftsNet.getListenerRegistry().call(fileLoadedEvent);
        if (fileLoadedEvent.isCancelled()) return;
        Path share = fileLoadedEvent.getPath().toAbsolutePath();

        if (!share.startsWith(folder) || Files.isDirectory(share)) {
            response.setCode(403);
            response.setContentType("text/html; charset=utf-8");
            response.print(DefaultPages.notallowed(domain, request.unsafe().getLocalAddress().getPort()));
            return;
        } else if (Files.notExists(share)) {
            response.setCode(404);
            response.setContentType("text/html; charset=utf-8");
            response.print(DefaultPages.notfound(domain, request.unsafe().getLocalAddress().getPort()));
            return;
        }

        response.setContentType(fileLoadedEvent.getContentType(), "text/plain");
        response.print(share);
    }

    /**
     * Responds to the client with an error message.
     *
     * @param response The Response object for managing the outgoing response.
     * @param message  The error message to be included in the response.
     */
    private static void respondWithError(Response response, int code, String message) {
        if (!response.headersSent()) response.setCode(code);
        if (!response.isBodyAble()) return;
        response.print(Json.empty().set("status", "" + code).set("message", message));
    }

}
