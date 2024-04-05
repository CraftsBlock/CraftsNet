package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.craftsblock.craftscore.cache.DoubleKeyedCache;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.annotations.transform.Transformer;
import de.craftsblock.craftsnet.api.annotations.transform.TransformerCollection;
import de.craftsblock.craftsnet.api.exceptions.TransformerException;
import de.craftsblock.craftsnet.api.interfaces.Transformable;
import de.craftsblock.craftsnet.events.RequestEvent;
import de.craftsblock.craftsnet.events.shares.ShareFileLoadedEvent;
import de.craftsblock.craftsnet.events.shares.ShareRequestEvent;
import de.craftsblock.craftsnet.logging.FileLogger;
import de.craftsblock.craftsnet.logging.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.craftsblock.craftsnet.utils.Utils.patternGroupNameExtractPattern;

/**
 * Handles HTTP requests and routes them to the appropriate handlers based on the registered routes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see WebServer
 * @since 3.0.1
 */
public class WebHandler implements HttpHandler {

    private static final Logger logger = CraftsNet.logger();
    private static final RouteRegistry registry = CraftsNet.routeRegistry();
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WebHandler.class);

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

            try {
                String ip;
                if (headers.containsKey("Cf-connecting-ip")) ip = headers.getFirst("Cf-connecting-ip");
                else if (headers.containsKey("X-forwarded-for")) ip = headers.getFirst("X-forwarded-for").split(", ")[0];
                else ip = exchange.getRemoteAddress().getAddress().getHostAddress();

                if (registry.isShare(url) && HttpMethod.parse(requestMethod) == HttpMethod.GET)
                    handleShare(response, exchange, domain, requestMethod, url, ip);
                else
                    handleRoute(response, exchange, domain, requestMethod, url, ip, headers);
            } catch (Exception e) {
                long errorID = FileLogger.createErrorLog(e, "http", url);
                logger.error(e, "Error: " + errorID);
                response.println(Json.empty()
                        .set("error.message", "An unexpected exception happened whilst processing your request!")
                        .set("error.identifier", errorID)
                        .asString());
            }
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
     * @throws IOException               If an I/O error occurs during request processing.
     * @throws InvocationTargetException If an error occurs while invoking the route handler.
     * @throws IllegalAccessException    If the route handler cannot be accessed.
     */
    private void handleRoute(Response response, HttpExchange exchange, String domain, String requestMethod, String url, String ip, Headers headers) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
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
        List<String> groupNames = getGroupNames(validator.pattern());

        args[0] = new Exchange(url, request, response);
        for (int i = 2; i <= matcher.groupCount(); i++)
            args[i - 1] = matcher.group(i);

        // Create a cache for transformed parameters
        DoubleKeyedCache<Class<? extends Transformable<?>>, String, Object> transformerCache = new DoubleKeyedCache<>(20);

        // Loop through all priorities
        ProcessPriority.Priority priority = ProcessPriority.Priority.LOWEST;
        while (priority != null) {
            if (routes.isEmpty()) break;

            // Loop through all registered routes
            Iterator<RouteRegistry.RouteMapping> iterator = routes.iterator();
            method:
            while (iterator.hasNext()) {
                RouteRegistry.RouteMapping mapping = iterator.next();
                if (!mapping.priority().equals(priority)) continue;
                iterator.remove();

                Object[] copiedArgs = new Object[args.length];
                System.arraycopy(args, 0, copiedArgs, 0, args.length);

                Object handler = mapping.handler();
                Method method = mapping.method();

                // Apply all the transformers
                Transformer standaloneTransformer = method.getAnnotation(Transformer.class);
                TransformerCollection transformers = method.getAnnotation(TransformerCollection.class);
                if (transformers != null)
                    for (Transformer transformer : transformers.value())
                        transform(groupNames, copiedArgs, transformer, transformerCache);
                else if (standaloneTransformer != null)
                    transform(groupNames, copiedArgs, standaloneTransformer, transformerCache);

                // Loop through all parameters of the method and checks the parameter type
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == copiedArgs.length)
                    for (int i = 1; i < parameterTypes.length; i++) {
                        Class<?> type = parameterTypes[i];
                        Object value = copiedArgs[i];

                        // Check if the value is an TransformerException and display it via the response if present.
                        if (value instanceof TransformerException e) {
                            response.println(Json.empty().set("error", "Could not process transformer: " + e.getMessage()).asString());
                            // Continue to the next route
                            continue method;
                        }

                        // Check if the parameter type is not the value type
                        if (!type.isAssignableFrom(value.getClass())) {
                            String[] name = type.getSimpleName().split("\\.");

                            // Gets and checks if a method for an alternative transformation is present.
                            // This allows for example the use of both Integer and int
                            Method converter = Utils.getMethod(value.getClass(), name[name.length - 1] + "Value");
                            if (converter == null) continue;

                            // Set the value of the argument to the return of the method for alternativ transformation.
                            copiedArgs[i] = converter.invoke(value);
                        }
                    }

                // Call the method of the route handler
                method.invoke(handler, copiedArgs);
            }

            // Update the current process priority
            priority = priority.next();
        }

        // Clear up the transformer cache to free up memory
        transformerCache.clear();
    }

    /**
     * Performs a transformation with an {@link Transformer}
     *
     * @param groupNames       A {@link List<String>} with all the named groups of the url validator
     * @param args             A {@link Object} array with all the dynamic url parameter values
     * @param transformer      The current {@link Transformer} used to transform an argument
     * @param transformerCache An {@link DoubleKeyedCache} which contains all the previous executed transformer results, for faster execution
     * @throws NoSuchMethodException  if the transformer method could not be not found
     * @throws InstantiationException if no new instance of the {@link Transformable<?>} can be created
     * @throws IllegalAccessException if the access to the constructor of the {@link Transformable<?>} or
     *                                if the access to the method {@link Transformable#transform(String)} is restricted
     */
    private static void transform(List<String> groupNames, Object[] args, Transformer transformer, DoubleKeyedCache<Class<? extends Transformable<?>>, String, Object> transformerCache) throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        String parameter = transformer.parameter();
        // Abort if the dynamic parameter is not present in the named groups
        if (!groupNames.contains(parameter)) {
            logger.warning("Parameter " + parameter + " has a transformer but is not used!");
            return;
        }

        // Load all important variables
        Class<? extends Transformable<?>> transformable = transformer.transformer();
        int groupIndex = groupNames.indexOf(parameter) + 1;
        String value = (String) args[groupIndex];

        // Check if the transformer is cacheable and the transformer cache contains this specific transformer
        if (transformer.cacheable() && transformerCache.containsKeyPair(transformable, value))
            // Override the args with the cache value from the transformer cache
            args[groupIndex] = transformerCache.get(transformable, value);
        else {
            try {
                // Search for the transform method on the transformable
                Method transformerMethod = Utils.getMethod(transformable, "transform", String.class);
                assert transformerMethod != null;

                // Create a new instance of the transformable
                Object owner = transformable.getDeclaredConstructor().newInstance();

                // Execute the transform method on the transformable and inject it into the args
                Object transformed = transformerMethod.invoke(owner, value);
                args[groupIndex] = transformed;
                // Put the transformed value into the cache, if the transformer is cacheable
                if (transformer.cacheable()) transformerCache.put(transformable, value, transformed);
            } catch (InvocationTargetException parent) {
                // Check if the cause of the InvocationTargetException is an TransformerException
                if (parent.getCause() instanceof TransformerException e)
                    // Parse up the TransformerException to the route handler
                    args[groupIndex] = e;
            }
        }
    }

    /**
     * Extracts the group names of a {@link Pattern}.
     *
     * @param regex The pattern, from which the group names should be extracted.
     * @return A {@link List<String>} which contains the group names in the right order.
     */
    private static List<String> getGroupNames(String regex) {
        Set<String> groupNames = new TreeSet<>();
        Matcher matcher = patternGroupNameExtractPattern.matcher(regex);
        while (matcher.find()) groupNames.add(matcher.group(1));
        List<String> output = new ArrayList<>(groupNames);
        Collections.reverse(output);
        return output;
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
     * @throws IOException               If an I/O error occurs during request processing.
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
        ShareRequestEvent event = new ShareRequestEvent(url, path);
        CraftsNet.listenerRegistry().call(event);
        if (event.isCancelled()) {
            logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[SHARE ABORTED]");
            return;
        }

        for (String key : event.getHeaders().keySet())
            event.getHeader(key).forEach(value -> response.addHeader(key, value));

        path = event.getFilePath();
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
