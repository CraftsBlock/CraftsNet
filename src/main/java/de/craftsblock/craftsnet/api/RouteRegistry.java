package de.craftsblock.craftsnet.api;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.RequestHandler;
import de.craftsblock.craftsnet.api.http.RequestMethod;
import de.craftsblock.craftsnet.api.http.Route;
import de.craftsblock.craftsnet.api.websocket.MessageReceiver;
import de.craftsblock.craftsnet.api.websocket.Socket;
import de.craftsblock.craftsnet.api.websocket.SocketHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The RouteRegistry class manages the registration and unregistration of request handlers (routes) and socket handlers.
 * It stores and maps the registered routes and sockets based on their patterns, allowing for efficient handling of incoming requests.
 *
 * @author CraftsBlock
 * @since 1.0.0
 */
public class RouteRegistry {

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
        for (Method method : Utils.getMethodByAnnotation(handler.getClass(), Route.class))
            try {
                if (method.getParameterCount() <= 0)
                    throw new IllegalStateException("Die Methode " + method.getName() + " ist mit " + Route.class.getName() + " versehen, hat aber nicht " + Exchange.class.getName() + " als erstes argument!");
                if (!Exchange.class.isAssignableFrom(method.getParameters()[0].getType()))
                    throw new IllegalStateException("Die Methode " + method.getName() + " ist mit " + Route.class.getName() + " versehen, hat aber nicht " + Exchange.class.getName() + " als erstes argument!");
                Route route = method.getAnnotation(Route.class);
                Pattern validator = createValidator(route.path());
                routes.put(
                        validator,
                        new RouteMapping(method, handler, route, validator)
                );
            } catch (Exception e) {
                e.printStackTrace();
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
        Socket socket = t.getClass().getAnnotation(Socket.class);
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
        for (Method method : Utils.getMethodByAnnotation(handler.getClass(), Route.class))
            try {
                Route route = method.getAnnotation(Route.class);
                routes.entrySet().removeIf(validator -> validator.getKey().matcher(url(route.path())).matches());
            } catch (Exception e) {
                e.printStackTrace();
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
        Socket socket = t.getClass().getAnnotation(Socket.class);
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
        return routes.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(url(url)).matches())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the route mapping associated with a specific URL and HTTP method.
     *
     * @param url    The URL for which the route mapping is sought.
     * @param method The HTTP method (GET, POST, etc.) for which the route mapping is sought.
     * @return The RouteMapping object associated with the URL and HTTP method, or null if no mapping is found.
     * @since 2.1.1
     */
    @Nullable
    public RouteMapping getRoute(String url, String method) {
        return routes.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(url(url)).matches() && RequestMethod.asString(entry.getValue().route.methods()).toUpperCase().contains(method.toUpperCase()))
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
        return sockets.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(url(url)).matches())
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
     * Checks if the registry has any registered socket handlers.
     *
     * @return true if the registry has registered socket handlers, false otherwise.
     * @since 2.1.1
     */
    public boolean hasWebsockets() {
        return sockets.size() != 0;
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

    /**
     * The RouteMapping class represents the mapping of a registered route.
     * It stores information about the method, handler, route annotation, and validator pattern for the route.
     *
     * @since 1.0.0
     */
    public record RouteMapping(@NotNull Method method, @NotNull Object handler, @NotNull Route route,
                               @NotNull Pattern validator) {
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
