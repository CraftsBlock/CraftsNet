package de.craftsblock.craftsnet.api;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.http.RequestHandler;
import de.craftsblock.craftsnet.api.http.annotations.RequestMethod;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.api.websocket.MessageReceiver;
import de.craftsblock.craftsnet.api.websocket.Socket;
import de.craftsblock.craftsnet.api.websocket.SocketHandler;
import de.craftsblock.craftsnet.utils.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The RouteRegistry class manages the registration and unregistration of request handlers (routes) and socket handlers.
 * It stores and maps the registered routes and sockets based on their patterns, allowing for efficient handling of incoming requests.
 *
 * @author CraftsBlock
 * @version 2.4
 * @since 1.0.0
 */
public class RouteRegistry {

    private static final Logger logger = CraftsNet.logger;
    private final ConcurrentHashMap<Pattern, RouteMapping> routes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Pattern, SocketMapping> sockets = new ConcurrentHashMap<>();

    /**
     * Registers a request handler (route) by inspecting its annotated methods and adding it to the registry.
     * The method should have Exchange as its first argument and be annotated with @Route.
     *
     * @param handler The RequestHandler to be registered.
     * @since 1.0.0
     */
    public void register(RequestHandler handler) {
        Route parent = annotation(handler, Route.class);
        for (Method method : Utils.getMethodByAnnotation(handler.getClass(), Route.class))
            try {
                if (method.getParameterCount() <= 0)
                    throw new IllegalStateException("Die Methode " + method.getName() + " ist mit " + Route.class.getName() + " versehen, hat aber nicht " + Exchange.class.getName() + " als erstes argument!");
                if (!Exchange.class.isAssignableFrom(method.getParameters()[0].getType()))
                    throw new IllegalStateException("Die Methode " + method.getName() + " ist mit " + Route.class.getName() + " versehen, hat aber nicht " + Exchange.class.getName() + " als erstes argument!");
                Route route = annotation(method, Route.class);
                Pattern validator = createValidator(url(parent != null ? parent.value() : "", route.value()));
                routes.put(
                        validator,
                        new RouteMapping(method, handler, validator)
                );
            } catch (Exception e) {
                logger.error(e);
            }
    }

    /**
     * Registers a socket handler by inspecting its annotated methods and adding it to the registry.
     * The method should be annotated with @{@link MessageReceiver} and @{@link Socket}.
     *
     * @param t   The SocketHandler to be registered.
     * @param <T> The type of the SocketHandler.
     * @since 2.1.1
     */
    public <T extends SocketHandler> void register(T t) {
        Socket socket = annotation(t, Socket.class);
        Pattern validator = createValidator(socket.path());
        sockets.put(
                validator,
                new SocketMapping(t, socket, validator, Utils.getMethodByAnnotation(t.getClass(), MessageReceiver.class).toArray(new Method[0]))
        );
    }

    /**
     * Unregisters a request handler (route) from the registry.
     *
     * @param handler The RequestHandler to be unregistered.
     * @since 1.0.0
     */
    public void unregister(RequestHandler handler) {
        Route parent = annotation(handler, Route.class);
        for (Method method : Utils.getMethodByAnnotation(handler.getClass(), Route.class))
            try {
                Route route = annotation(method, Route.class);
                routes.entrySet().removeIf(validator -> validator.getKey().matcher(url(parent != null ? parent.value() : "", route.value())).matches());
            } catch (Exception e) {
                logger.error(e);
            }
    }

    /**
     * Unregisters a socket handler from the registry.
     *
     * @param t   The SocketHandler to be unregistered.
     * @param <T> The type of the SocketHandler.
     * @since 2.1.1
     */
    public <T extends SocketHandler> void unregister(T t) {
        Socket socket = annotation(t, Socket.class);
        sockets.entrySet().removeIf(validator -> validator.getKey().matcher(url(socket.path())).matches());
    }

    /**
     * Gets an immutable copy of the registered routes in the registry.
     *
     * @return A ConcurrentHashMap containing the registered routes.
     * @since 1.0.0
     */
    @NotNull
    public ConcurrentHashMap<Pattern, RouteMapping> getRoutes() {
        return (ConcurrentHashMap<Pattern, RouteMapping>) Map.copyOf(routes);
    }

    /**
     * Gets the route mapping associated with a specific URL.
     *
     * @param url The URL for which the route mapping is sought.
     * @return The RouteMapping object associated with the URL, or null if no mapping is found.
     * @since 1.0.0
     */
    @Nullable
    public RouteMapping getRoute(String url) {
        return getRoute(url, null, null);
    }

    /**
     * Gets the route mapping associated with a specific URL and HTTP method.
     *
     * @param url The URL for which the route mapping is sought.
     * @return The RouteMapping object associated with the URL, or null if no mapping is found.
     * @since 2.1.1
     */
    @Nullable
    public RouteMapping getRoute(String url, String method) {
        return getRoute(url, null, method);
    }

    /**
     * Gets the route mapping associated with a specific URL, domain and HTTP method.
     *
     * @param url    The URL for which the route mapping is sought.
     * @param domain The domain for which the route mapping is sought.
     * @param method The HTTP method (GET, POST, etc.) for which the route mapping is sought.
     * @return The RouteMapping object associated with the URL and HTTP method, or null if no mapping is found.
     * @since 2.3.0
     */
    @Nullable
    public RouteMapping getRoute(String url, String domain, String method) {
        return routes.entrySet().stream()
                .filter(entry -> {
                    try {
                        Method tmp = entry.getValue().method();
                        HttpMethod[] methods = annotation(tmp, RequestMethod.class, HttpMethod[].class);
                        List<String> domains = new ArrayList<>();
                        addArray(annotation(tmp, Domain.class, String[].class), domains);
                        addArray(annotation(entry.getValue().handler, Domain.class, String[].class), domains);
                        removeDuplicates(domains);
                        if (domains.isEmpty()) domains.add("*");

                        return entry.getKey().matcher(url(url)).matches() &&
                                (methods == null || HttpMethod.asString(methods).toUpperCase().contains(method.toUpperCase())) &&
                                (domain == null || domains.contains("*") || domains.contains(domain));
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                             InstantiationException e) {
                        logger.error(e, "Error whilst loading http route");
                    }
                    return false;
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets an immutable copy of the registered socket handlers in the registry.
     *
     * @return A ConcurrentHashMap containing the registered socket handlers.
     * @since 2.1.1
     */
    @NotNull
    public ConcurrentHashMap<Pattern, SocketMapping> getSockets() {
        return sockets;
    }

    /**
     * Gets the socket mapping associated with a specific URL.
     *
     * @param url The URL for which the socket mapping is sought.
     * @return The SocketMapping object associated with the URL, or null if no mapping is found.
     * @since 2.1.1
     */
    @Nullable
    public SocketMapping getSocket(String url) {
        return getSocket(url, null);
    }

    /**
     * Gets the socket mapping associated with a specific URL and domain.
     *
     * @param url    The URL for which the socket mapping is sought.
     * @param domain The domain for which the socket mapping is sought.
     * @return The SocketMapping object associated with the URL, or null if no mapping is found.
     * @since 2.1.1
     */
    @Nullable
    public SocketMapping getSocket(String url, String domain) {
        return sockets.entrySet().stream()
                .filter(entry -> {
                    try {
                        List<String> domains = new ArrayList<>();
                        addArray(annotation(entry.getValue(), Domain.class, String[].class), domains);
                        addArray(annotation(entry.getValue().handler, Domain.class, String[].class), domains);
                        removeDuplicates(domains);
                        if (domains.isEmpty()) domains.add("*");

                        return entry.getKey().matcher(url(url)).matches() &&
                                (domain == null || domains.contains("*") || domains.contains(domain));
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                             InstantiationException e) {
                        logger.error(e, "Error whilst loading socket route");
                    }
                    return false;
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates a validator pattern for a given URL.
     *
     * @param url The URL for which the validator pattern is created.
     * @return The Pattern object representing the validator pattern.
     * @since 1.0.0
     */
    @NotNull
    private Pattern createValidator(String url) {
        Pattern pattern = Pattern.compile("\\{(.*?[^/]+)\\}", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(url(url));
        return Pattern.compile("^(" + matcher.replaceAll("(?<$1>[^/]+)") + ")/?", Pattern.CASE_INSENSITIVE);
    }

    /**
     * This method searches for an annotation of the specified type in an object.
     *
     * @param o     The object in which to search for the annotation.
     * @param clazz The class of the annotation to search for.
     * @param <A>   The type of the annotation.
     * @return The found annotation or null if none is found.
     * @since 2.3.0
     */
    private <A extends Annotation> A annotation(Object o, Class<A> clazz) {
        if (o instanceof Method method) return method.getAnnotation(clazz);
        return o.getClass().getAnnotation(clazz);
    }

    /**
     * Retrieves the value of a specified annotation attribute from an object.
     *
     * @param <A>   The type of the annotation.
     * @param <R>   The type of the attribute value.
     * @param o     The object containing the annotation.
     * @param clazz The class of the annotation.
     * @param type  The class of the attribute value.
     * @return The value of the specified annotation attribute.
     * @throws NoSuchMethodException     If the attribute's getter method is not found.
     * @throws InvocationTargetException If there is an issue invoking the getter method.
     * @throws IllegalAccessException    If there is an access issue with the getter method.
     */
    private <A extends Annotation, R> R annotation(Object o, Class<A> clazz, Class<R> type) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        A annotation = annotation(o, clazz);
        if (annotation == null) {
            Method method = clazz.getDeclaredMethod("value");
            if(method.getDefaultValue() == null) return null;
            return (R) method.getDefaultValue();
        }
        Method method = annotation.getClass().getDeclaredMethod("value");
        R value = (R) method.invoke(annotation);
        if (value == null) value = (R) method.getDefaultValue();
        return value;
    }

    /**
     * Concatenates two URL path segments, ensuring proper formatting.
     *
     * @param parent The parent path segment.
     * @param child  The child path segment to append.
     * @return The concatenated URL path.
     */
    private String url(String parent, String child) {
        parent = parent.trim();
        child = child.trim();

        String result = (!parent.startsWith("/") && !parent.isBlank() ? "/" : "") + parent;
        if (!parent.endsWith("/")) result += "/";
        result += (child.startsWith("/") ? child.substring(1) : child);
        return result;
    }

    /**
     * Checks if the registry has any registered socket handlers.
     *
     * @return true if the registry has registered socket handlers, false otherwise.
     * @since 2.1.1
     */
    public boolean hasWebsockets() {
        return !sockets.isEmpty();
    }

    /**
     * Checks if the registry has any registered route handlers.
     *
     * @return true if the registry has registered route handlers, false otherwise.
     * @since 2.1.1
     */
    public boolean hasRoutes() {
        return !routes.isEmpty();
    }

    /**
     * Formats the URL by ensuring it starts with a slash.
     *
     * @param url The URL to be formatted.
     * @return The formatted URL.
     * @since 1.0.0
     */
    private String url(String url) {
        return (!url.trim().startsWith("/") ? "/" : "") + url.trim();
    }

    private <T> void removeDuplicates(List<T> list) {
        HashSet<T> unique = new HashSet<>(list);
        list.clear();
        list.addAll(unique);
    }

    private <T> void addArray(T[] t, List<T> list) {
        if(t == null) return;
        list.addAll(Arrays.asList(t));
    }

    /**
     * The RouteMapping class represents the mapping of a registered route.
     * It stores information about the method, handler, and validator pattern for the route.
     *
     * @version 1.1
     * @since 1.0.0
     */
    public record RouteMapping(@NotNull Method method, @NotNull Object handler, @NotNull Pattern validator) {
    }

    /**
     * The SocketMapping class represents the mapping of a registered socket handler.
     * It stores information about the socket handler, socket annotation, validator pattern, and message receiver methods.
     *
     * @since 2.1.1
     */
    public record SocketMapping(@NotNull SocketHandler handler, @NotNull Socket socket, @NotNull Pattern validator,
                                Method[] receiver) {
    }

}
