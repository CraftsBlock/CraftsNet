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

public class RouteRegistry {

    private final ConcurrentHashMap<Pattern, RouteMapping> routes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Pattern, SocketMapping> sockets = new ConcurrentHashMap<>();

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

    public <T extends SocketHandler> void register(T t) {
        Socket socket = t.getClass().getAnnotation(Socket.class);
        Pattern validator = createValidator(socket.path());
        sockets.put(
                validator,
                new SocketMapping(t, socket, validator, Utils.getMethodByAnnotation(t.getClass(), MessageReceiver.class).toArray(new Method[0]))
        );
    }

    public void unregister(RequestHandler handler) {
        for (Method method : Utils.getMethodByAnnotation(handler.getClass(), Route.class))
            try {
                Route route = method.getAnnotation(Route.class);
                routes.entrySet().removeIf(validator -> validator.getKey().matcher(url(route.path())).matches());
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public <T extends SocketHandler> void unregister(T t) {
        Socket socket = t.getClass().getAnnotation(Socket.class);
        sockets.entrySet().removeIf(validator -> validator.getKey().matcher(url(socket.path())).matches());
    }

    @NotNull
    public ConcurrentHashMap<Pattern, RouteMapping> getRoutes() {
        return (ConcurrentHashMap<Pattern, RouteMapping>) Map.copyOf(routes);
    }

    @Nullable
    public RouteMapping getRoute(String url) {
        return routes.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(url(url)).matches())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public RouteMapping getRoute(String url, String method) {
        return routes.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(url(url)).matches() && RequestMethod.asString(entry.getValue().route.methods()).toUpperCase().contains(method.toUpperCase()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public ConcurrentHashMap<Pattern, SocketMapping> getSockets() {
        return sockets;
    }

    @Nullable
    public SocketMapping getSocket(String url) {
        return sockets.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(url(url)).matches())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    @NotNull
    private Pattern createValidator(String url) {
        Pattern pattern = Pattern.compile("\\{(.*?[^/]+)\\}", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(url(url));
        return Pattern.compile("^(" + matcher.replaceAll("(?<$1>[^/]+)") + ")/?", Pattern.CASE_INSENSITIVE);
    }

    public boolean hasWebsockets() {
        return sockets.size() != 0;
    }

    private String url(String url) {
        return (!url.trim().startsWith("/") ? "/" : "") + url.trim();
    }

    public record RouteMapping(@NotNull Method method, @NotNull Object handler, @NotNull Route route,
                               @NotNull Pattern validator) {
    }

    public record SocketMapping(@NotNull SocketHandler handler, @NotNull Socket socket, @NotNull Pattern validator,
                                Method[] receiver) {
    }

}
