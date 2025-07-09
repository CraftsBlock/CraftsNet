package de.craftsblock.craftsnet.api;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.http.*;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.api.http.builtin.DefaultRoute;
import de.craftsblock.craftsnet.api.middlewares.Middleware;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.requirements.Requirement;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementInfo;
import de.craftsblock.craftsnet.api.websocket.*;
import de.craftsblock.craftsnet.api.websocket.annotations.Socket;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The RouteRegistry class manages the registration and unregistration of {@link RequestHandler} (routes) and {@link SocketHandler} (websockets).
 * It stores and maps the registered routes and sockets based on their patterns, allowing for efficient handling of incoming requests.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 3.5.1
 * @since 1.0.0-SNAPSHOT
 */
public class RouteRegistry {

    private final CraftsNet craftsNet;

    private final ConcurrentHashMap<Class<? extends Server>, ConcurrentHashMap<Pattern, ConcurrentLinkedQueue<EndpointMapping>>> serverMappings = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Pattern, ShareMapping> shares = new ConcurrentHashMap<>();

    /**
     * Constructs a new instance of the RouteRegistry
     *
     * @param craftsNet The CraftsNet instance which instantiates this route registry
     */
    public RouteRegistry(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Registers an endpoint handler ({@link RequestHandler} and or {@link SocketHandler}) by inspecting its annotated methods and adding it to the registry.
     *
     * @param handler The Handler to be registered.
     */
    public void register(Handler handler) {
        if (isRegistered(handler)) return;
        ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> annotations = retrieveHandlerInfoMap(handler.getClass());

        for (Class<? extends Annotation> annotation : annotations.keySet())
            try {
                ServerMapping serverMapping = annotations.get(annotation);
                Class<? extends Server> rawServer = serverMapping.rawServer();
                var endpoints = serverMappings.computeIfAbsent(rawServer, c -> new ConcurrentHashMap<>());
                Collection<Class<? extends Annotation>> requirementAnnotations = new ArrayList<>(craftsNet.getRequirementRegistry()
                        .getRequirements(rawServer).parallelStream().map(Requirement::getAnnotation).toList());

                String parent = ReflectionUtils.retrieveValueOfAnnotation(handler.getClass(), annotation, String.class, true);

                // Continue if no handlers exists for this server type
                if (!Utils.hasMethodsWithAnnotation(handler.getClass(), annotation)) continue;

                for (Method method : Utils.getMethodsByAnnotation(handler.getClass(), annotation)) {
                    if (WebServer.class.isAssignableFrom(rawServer)) {
                        if (method.getParameterCount() <= 0)
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() +
                                    " but does not require " + Exchange.class.getName() + " as the first parameter!");

                        if (!Exchange.class.isAssignableFrom(method.getParameterTypes()[0]))
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() +
                                    " but does not require " + Exchange.class.getName() + " as the first parameter!");
                    } else {
                        if (method.getParameterCount() <= 1)
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() +
                                    " but does not require " + SocketExchange.class.getName() + " and a data type as parameters!");

                        if (!SocketExchange.class.isAssignableFrom(method.getParameterTypes()[0]))
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() +
                                    " but does not require " + SocketExchange.class.getName() + " as the first parameter!");

                        if (!String.class.isAssignableFrom(method.getParameterTypes()[1]) &&
                                !byte[].class.isAssignableFrom(method.getParameterTypes()[1]) &&
                                !Frame.class.isAssignableFrom(method.getParameterTypes()[1]) &&
                                !ByteBuffer.class.isAssignableFrom(method.getParameterTypes()[1]))
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() +
                                    " but does not require a Frame, ByteBuffer, String or byte[] as the second parameter!");
                    }

                    String child = ReflectionUtils.retrieveValueOfAnnotation(method, annotation, String.class, true);
                    ProcessPriority priority = ReflectionUtils.retrieveRawAnnotation(method, ProcessPriority.class);
                    Pattern validator = createOrGetValidator(mergeUrl(parent != null ? parent : "", child), endpoints);

                    // Load requirements
                    ConcurrentHashMap<Class<? extends Annotation>, RequirementInfo> requirements = new ConcurrentHashMap<>();
                    craftsNet.getRequirementRegistry().loadRequirements(requirements, requirementAnnotations, method, handler);

                    Stack<Middleware> middlewares = craftsNet.getMiddlewareRegistry().resolveMiddlewares(handler, method);

                    // Register the endpoint mapping
                    ConcurrentLinkedQueue<EndpointMapping> mappings = endpoints.computeIfAbsent(validator, pattern -> new ConcurrentLinkedQueue<>());
                    mappings.add(new EndpointMapping(
                            priority != null ? priority.value() : ProcessPriority.Priority.NORMAL,
                            method, handler, validator, requirements, middlewares
                    ));
                }

            } catch (Exception e) {
                throw new RuntimeException("Could not correctly register handlers for @%s of %s!".formatted(
                        annotation.getSimpleName(), handler.getClass().getSimpleName()
                ), e);
            }

        // Unregister the DefaultRoute
        if (!(handler instanceof DefaultRoute) && (hasRoutes() || hasShares() || hasWebsockets()))
            this.unregister(DefaultRoute.getInstance());

        // Loop through all active servers and turn them on as they are now needed.
        for (ServerMapping mapping : annotations.values()) {
            Server server = mapping.server(craftsNet);
            if (server == null) continue;
            server.awakeOrWarn();
        }
    }

    /**
     * Checks if the given {@link Handler} is registered.
     * This class is a wrapper for {@link RouteRegistry#isRegistered(Class)}.
     *
     * @param handler The {@link Handler} to check.
     * @return {@code true} when the {@link Handler} was registered, {@code false} otherwise.
     * @since 3.2.1-SNAPSHOT
     */
    public boolean isRegistered(Handler handler) {
        return isRegistered(handler.getClass());
    }

    /**
     * Checks if the given class representation of the {@link Handler} is registered.
     *
     * @param type The class representation of the {@link Handler} to check.
     * @return {@code true} when the {@link Handler} was registered, {@code false} otherwise.
     * @since 3.2.1-SNAPSHOT
     */
    public boolean isRegistered(Class<? extends Handler> type) {
        if (serverMappings.isEmpty()) return false;

        return retrieveHandlerInfoMap(type).values().stream()
                .map(ServerMapping::rawServer).filter(serverMappings::containsKey)
                .map(serverMappings::get).filter(Objects::nonNull)
                .filter(map -> !map.isEmpty())
                .flatMap(map -> map.values().stream())
                .filter(list -> !list.isEmpty())
                .flatMap(Collection::stream)
                .map(EndpointMapping::handler)
                .anyMatch(type::isInstance);
    }

    /**
     * Shares a folder for a specified path.
     *
     * @param path   The path pattern to share.
     * @param folder The folder to be shared.
     * @throws IllegalArgumentException If the provided "folder" is not a directory.
     */
    public void share(String path, File folder) {
        this.share(path, folder.toPath());
    }

    /**
     * Shares a folder for a specified path.
     *
     * @param path   The path pattern to share.
     * @param folder The folder to be shared.
     * @throws IllegalArgumentException If the provided "folder" is not a directory.
     * @since 3.3.5-SNAPSHOT
     */
    public void share(String path, Path folder) {
        this.share(path, folder, true);
    }

    /**
     * Shares a folder for a specified path.
     *
     * @param path    The path pattern to share.
     * @param folder  The folder to be shared.
     * @param onlyGet Set to true if only get requests should be received by this share, false otherwise.
     * @throws IllegalArgumentException If the provided "folder" is not a directory.
     */
    public void share(String path, File folder, boolean onlyGet) {
        this.share(path, folder.toPath(), onlyGet);
    }

    /**
     * Shares a folder for a specified path.
     *
     * @param path    The path pattern to share.
     * @param folder  The path of the folder to be shared.
     * @param onlyGet Set to true if only get requests should be received by this share, false otherwise.
     * @throws IllegalArgumentException If the provided "folder" is not a directory.
     * @since 3.3.5-SNAPSHOT
     */
    public void share(String path, Path folder, boolean onlyGet) {
        if (!Files.isDirectory(folder))
            throw new IllegalArgumentException("\"folder\" must be a folder!");

        Pattern pattern = Pattern.compile(formatUrl(path) + "/" + "?(.*)");
        shares.put(pattern, new ShareMapping(folder.toAbsolutePath().toString(), onlyGet));

        // Only continue if the web server was set
        if (this.craftsNet.getWebServer() != null)
            this.craftsNet.getWebServer().awakeOrWarn();
    }

    /**
     * Unregisters an endpoint handler (route or websocket) from the registry.
     *
     * @param handler The RequestHandler to be unregistered.
     */
    public void unregister(final Handler handler) {
        if (!isRegistered(handler)) return;

        ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> annotations = retrieveHandlerInfoMap(handler.getClass());

        for (Class<? extends Annotation> annotation : annotations.keySet())
            try {
                ServerMapping mapping = annotations.get(annotation);

                ConcurrentHashMap<Pattern, ConcurrentLinkedQueue<EndpointMapping>> endpoints = serverMappings.computeIfAbsent(mapping.rawServer(), c -> new ConcurrentHashMap<>());
                String parent = ReflectionUtils.retrieveValueOfAnnotation(handler.getClass(), annotation, String.class, true);

                // Continue if no handlers exists for this server type
                if (!Utils.hasMethodsWithAnnotation(handler.getClass(), annotation)) continue;

                for (Method method : Utils.getMethodsByAnnotation(handler.getClass(), annotation)) {
                    String child = ReflectionUtils.retrieveValueOfAnnotation(method, annotation, String.class, true);
                    endpoints.entrySet().stream()
                            .filter(entry -> entry.getKey().matcher(mergeUrl(parent != null ? parent : "", child)).matches())
                            .peek(entry -> entry.getValue().removeIf(endpointMapping -> endpointMapping.handler().equals(handler)))
                            .forEach(entry -> {
                                if (!endpoints.containsKey(entry.getKey()) || !entry.getValue().isEmpty()) return;
                                endpoints.remove(entry.getKey());
                            });
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not correctly unregister handlers for @%s of %s!".formatted(
                        annotation.getSimpleName(), handler.getClass().getSimpleName()
                ), e);
            }

        // Loop through all active servers and turn them off if they are not needed.
        for (ServerMapping mapping : annotations.values()) {
            Server server = mapping.server(craftsNet);
            if (server == null) continue;
            server.sleepIfNotNeeded();
        }
    }

    /**
     * Returns an immutable copy of the shared folders and patterns.
     *
     * @return An immutable copy of the shared folders and patterns.
     */
    public Map<Pattern, File> getShares() {
        return shares.entrySet().parallelStream()
                .map(entry -> Map.entry(entry.getKey(), new File(entry.getValue().filepath())))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets the mapping for the share associated with the given URL.
     *
     * @param url The URL for which to retrieve the associated mapping.
     * @return The mapping of the share or null if no match is found.
     */
    public ShareMapping getShare(String url) {
        return shares.entrySet().parallelStream()
                .filter(entry -> entry.getKey().matcher(formatUrl(url)).matches())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the shared folder associated with the given URL.
     *
     * @param url The URL for which to retrieve the associated shared folder.
     * @return The shared folder or null if no match is found.
     */
    public Path getShareFolder(String url) {
        if (!isShare(url)) return null;
        return Path.of(getShare(url).filepath());
    }

    /**
     * Gets the pattern associated with the shared folder that matches the given URL.
     *
     * @param url The URL for which to retrieve the associated pattern.
     * @return The pattern or null if no match is found.
     */
    public Pattern getSharePattern(String url) {
        return shares.keySet().parallelStream()
                .filter(file -> file.matcher(formatUrl(url)).matches())
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a URL corresponds to a shared folder by verifying if both a shared folder and its pattern exist for the URL.
     *
     * @param url The URL to check.
     * @return True if the URL corresponds to a shared folder, false otherwise.
     */
    public boolean isShare(String url) {
        return getShare(url) != null;
    }

    /**
     * Checks if a URL corresponding share is able to accept a specific http method.
     *
     * @param url    The URL to the corresponding share.
     * @param method The http method to check.
     * @return True if the share can accept the specific http method, false otherwise.
     */
    public boolean canShareAccept(String url, HttpMethod method) {
        if (!isShare(url)) return false;
        boolean onlyGet = getShare(url).onlyGet();
        return method.equals(HttpMethod.GET) || !onlyGet;
    }

    /**
     * Gets an immutable copy of the registered routes in the registry.
     *
     * @return A ConcurrentHashMap containing the registered routes.
     */
    @NotNull
    public Map<Pattern, ConcurrentLinkedQueue<EndpointMapping>> getRoutes() {
        return getEndpoints(WebServer.class);
    }

    /**
     * Gets the route mappings associated with a specific request information.
     *
     * @param request The http request for which a routes should be found.
     * @return A list of RouteMapping objects associated with the URL and HTTP method, or null if no mappings are found.
     */
    @Nullable
    public EnumMap<ProcessPriority.Priority, List<EndpointMapping>> getRoute(Request request) {
        return getEndpoint(WebServer.class, request.getUrl(), request);
    }

    /**
     * Checks if there are any registered route mappings for a given {@link Request}.
     *
     * @param request The {@link Request} to check.
     * @return {@code true} if a route mapping exists, otherwise {@code false}.
     * @since 3.3.3-SNAPSHOT
     */
    public boolean hasRouteMappings(Request request) {
        return hasEndpoint(WebServer.class, request.getUrl(), request);
    }

    /**
     * Gets an immutable copy of the registered socket handlers in the registry.
     *
     * @return A ConcurrentHashMap containing the registered socket handlers.
     */
    @NotNull
    public Map<Pattern, ConcurrentLinkedQueue<EndpointMapping>> getSockets() {
        return getEndpoints(WebSocketServer.class);
    }

    /**
     * Gets the socket mapping associated with a specific URL and domain.
     *
     * @param client The client for which the socket mapping is sought.
     * @return A list of SocketMapping objects associated with the URL, or null if no mapping is found.
     */
    @Nullable
    public EnumMap<ProcessPriority.Priority, List<EndpointMapping>> getSocket(WebSocketClient client) {
        return getEndpoint(WebSocketServer.class, client.getPath(), client);
    }

    /**
     * Checks if there are any registered websocket mappings for a given {@link WebSocketClient}.
     *
     * @param client The {@link WebSocketClient} to check.
     * @return {@code true} if a websocket mapping exists, otherwise {@code false}.
     * @since 3.3.3-SNAPSHOT
     */
    public boolean hasSocketMappings(WebSocketClient client) {
        return hasEndpoint(WebSocketServer.class, client.getPath(), client);
    }

    /**
     * Creates a validator pattern for a given URL.
     *
     * @param url The URL for which the validator pattern is created.
     * @return The Pattern object representing the validator pattern.
     */
    @NotNull
    private Pattern createValidator(String url) {
        Pattern pattern = Pattern.compile("\\{(.*?[^/]+)}", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(formatUrl(url));
        return Pattern.compile("^(" + matcher.replaceAll("(?<$1>[^/]+)") + ")/?", Pattern.CASE_INSENSITIVE);
    }

    /**
     * Checks if a pattern was already generated for this url and return the already generated pattern, otherwise a new one will be returned.
     *
     * @param url      The url for which a validator will be created.
     * @param mappings The map of mappings to check for the validator.
     * @return The existing pattern or a new one.
     */
    @NotNull
    private Pattern createOrGetValidator(String url, ConcurrentHashMap<Pattern, ?> mappings) {
        // @FixMe: Check if the group Names also match before returning the same pattern!
        return createValidator(url);
//        return mappings.keySet().parallelStream()
//                .filter(pattern -> pattern.matcher(url).matches())
//                .findFirst().orElse(createValidator(url));
    }

    /**
     * Gets the {@link EndpointMapping} associated with specific endpoint information.
     *
     * @param server The {@link Server} from which the endpoints should be loaded.
     * @param url    The url used to access the endpoint.
     * @param target The {@link RequireAble} containing the data about the request.
     * @return A {@link EnumMap} containing all matching {@link EndpointMapping} objects grouped by their corresponding {@link ProcessPriority.Priority}.
     */
    private EnumMap<ProcessPriority.Priority, List<EndpointMapping>> getEndpoint(Class<? extends Server> server, String url, RequireAble target) {
        return getFilteredEndpointStream(server, url, target).collect(Collectors.groupingBy(
                mapping -> mapping.priority,
                () -> new EnumMap<>(ProcessPriority.Priority.class),
                Collectors.toList()
        ));
    }

    /**
     * Checks if an endpoint exists for a given server type, url, and target.
     *
     * @param server The server class type.
     * @param url    The url or path to check.
     * @param target The {@link RequireAble} containing the data about the request.
     * @return {@code true} if an endpoint exists, otherwise {@code false}.
     * @since 3.3.3-SNAPSHOT
     */
    private boolean hasEndpoint(Class<? extends Server> server, String url, RequireAble target) {
        return getFilteredEndpointStream(server, url, target).findFirst().isPresent();
    }

    /**
     * Retrieves a filtered stream of endpoint mappings based on the given server type, url, and target.
     *
     * @param server The server class type.
     * @param url    The url or path to filter endpoints for.
     * @param target The {@link RequireAble} containing the data about the request.
     * @return A {@link Stream} of matching {@link EndpointMapping} objects.
     * @since 3.3.3-SNAPSHOT
     */
    private Stream<EndpointMapping> getFilteredEndpointStream(Class<? extends Server> server, String url, RequireAble target) {
        return getEndpoints(server).entrySet().parallelStream()
                .filter(entry -> entry.getKey().matcher(formatUrl(url)).matches())
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .filter(mapping -> craftsNet.getRequirementRegistry().getRequirements(server).parallelStream()
                        .filter(requirement ->
                                Utils.checkForMethod(requirement.getClass(), "applies", target.getClass(), EndpointMapping.class))
                        .map(requirement -> {
                            var method = Utils.getMethod(requirement.getClass(), "applies", target.getClass(), EndpointMapping.class);
                            return Map.entry(requirement, Objects.requireNonNull(method));
                        })
                        .allMatch(requirementEntry -> {
                            var requirement = requirementEntry.getKey();
                            var method = requirementEntry.getValue();

                            try {
                                return (boolean) method.invoke(requirement, target, mapping);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException("Could not apply requirement %s to %s#%s(%s)!".formatted(
                                        requirement.getClass().getSimpleName(), method.getDeclaringClass().getSimpleName(), method.getName(),
                                        String.join(", ", Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toList())
                                ), e);
                            }
                        })
                );
    }

    /**
     * Gets an immutable copy of the registered endpoints for the specific server type in the registry.
     *
     * @param server The {@link Server} from which the endpoints should be loaded.
     * @return A {@link ConcurrentHashMap} containing the registered endpoints.
     * @since 3.3.3-SNAPSHOT
     */
    @NotNull
    public Map<Pattern, ConcurrentLinkedQueue<EndpointMapping>> getEndpoints(Class<? extends Server> server) {
        return Map.copyOf(serverMappings.computeIfAbsent(server, c -> new ConcurrentHashMap<>()));
    }

    /**
     * Checks if the registry has any registered socket handlers.
     *
     * @return true if the registry has registered socket handlers, false otherwise.
     */
    public boolean hasWebsockets() {
        return !getSockets().isEmpty();
    }

    /**
     * Checks if the registry has any registered route handlers or shares.
     *
     * @return true if the registry has registered route handlers or shares, false otherwise.
     */
    public boolean hasRoutes() {
        return !getRoutes().isEmpty() || hasShares();
    }

    /**
     * Checks if the registry has any registered shares.
     *
     * @return true if the registry has registered shares, false otherwise.
     */
    public boolean hasShares() {
        return !shares.isEmpty();
    }

    /**
     * Concatenates two URL path segments, ensuring proper formatting.
     *
     * @param first  The first path segment.
     * @param second The second path segment to append.
     * @return The concatenated URL path.
     */
    private String mergeUrl(String first, String second) {
        first = first.trim();
        second = second.trim();

        String result = (!first.startsWith("/") && !first.isBlank() ? "/" : "") + first;
        if (!first.endsWith("/")) result += "/";
        result += second;

        return formatUrl(result);
    }

    /**
     * Formats the URL by ensuring it starts and does not end with a slash.
     * It also removes duplicate slashes from the url.
     *
     * @param url The URL to be formatted.
     * @return The formatted URL.
     */
    private String formatUrl(String url) {
        String result = (!url.trim().startsWith("/") ? "/" : "") + url.trim();
        return result
                .replaceAll("/+", "/")
                .replaceAll("/+$", "");
    }

    /**
     * Retrieves a map with information about the server types held by the handler type.
     *
     * @param handler The handler type from which the information should be retrieved.
     * @return The information about the server types held by the handler.
     */
    private ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> retrieveHandlerInfoMap(Class<? extends Handler> handler) {
        ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> annotations = new ConcurrentHashMap<>();
        if (RequestHandler.class.isAssignableFrom(handler))
            annotations.computeIfAbsent(Route.class, c -> new ServerMapping(WebServer.class));

        if (SocketHandler.class.isAssignableFrom(handler))
            annotations.computeIfAbsent(Socket.class, c -> new ServerMapping(WebSocketServer.class));

        if (annotations.isEmpty())
            throw new IllegalStateException("Invalid handler type " + handler.getClass().getSimpleName() + " only RequestHandler and SocketHandler are allowed!");
        return annotations;
    }

    /**
     * Retrieves all registered server mappings.
     *
     * @return A map which contains all registered endpoint mappings sorted per server.
     * @since 3.2.1-SNAPSHOT
     */
    @ApiStatus.Internal
    public ConcurrentHashMap<Class<? extends Server>, ConcurrentHashMap<Pattern, ConcurrentLinkedQueue<EndpointMapping>>> getServerMappings() {
        return new ConcurrentHashMap<>(Collections.unmodifiableMap(serverMappings));
    }

    /**
     * Retrieves the instance of {@link CraftsNet} bound to the registry.
     *
     * @return The instance of {@link CraftsNet} bound to the registry.
     * @since 3.2.1-SNAPSHOT
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

    /**
     * Represents the mapping of a registered endpoint handler.
     *
     * <p>This class stores information about the endpoint's processing priority, the associated handler method,
     * its parent handler, a validation pattern, and any requirement-related metadata.</p>
     *
     * @param priority     The {@link ProcessPriority.Priority} level for this endpoint.
     * @param method       The {@link Method} associated with the handler.
     * @param handler      The {@link Handler} instance that owns the method.
     * @param validator    The {@link Pattern} used for validating input related to the endpoint.
     * @param requirements A concurrent map of requirements, indexed by their annotation class.
     * @version 2.0.0
     * @since 3.0.5-SNAPSHOT
     */
    public record EndpointMapping(@NotNull ProcessPriority.Priority priority, @NotNull Method method, @NotNull Handler handler,
                                  @NotNull Pattern validator, ConcurrentHashMap<Class<? extends Annotation>, RequirementInfo> requirements,
                                  Stack<Middleware> middlewares) implements Mapping {

        /**
         * Checks whether the given annotation is present in the requirements.
         *
         * @param annotation The annotation class to check for.
         * @return {@code true} if the annotation is present, {@code false} otherwise.
         */
        @Override
        public <A extends Annotation> boolean isPresent(@NotNull Class<A> annotation) {
            return requirements.containsKey(annotation);
        }

        /**
         * Checks whether a specific key within the given annotation's requirements is present.
         *
         * @param annotation The annotation class to check for.
         * @param key        The specific key to check within the annotation's requirements.
         * @return {@code true} if the key is present within the annotation's requirements, {@code false} otherwise.
         */
        @Override
        public <A extends Annotation> boolean isPresent(@NotNull Class<A> annotation, @NotNull String key) {
            return isPresent(annotation) && requirements.get(annotation).hasValue(key);
        }

        /**
         * Retrieves a specific value from the requirements of the given annotation.
         *
         * @param annotation The annotation class to retrieve the value from.
         * @param key        The specific key within the annotation's requirements.
         * @return The value associated with the given key, or {@code null} if not present.
         */
        @Override
        public <A extends Annotation, T> T getRequirements(@NotNull Class<A> annotation, @NotNull String key) {
            if (!requirements.containsKey(annotation)) return null;
            return requirements.get(annotation).getValue(key);
        }

    }

    /**
     * The ShareMapping class represents the mapping of a registered shared endpoint.
     * It stores information about the filesystem path and if only get requests should be processed.
     *
     * @since 3.0.3-SNAPSHOT
     */
    public record ShareMapping(@NotNull String filepath, boolean onlyGet) implements Mapping {
    }

    /**
     * The ServerMapping class represents a mapping of a server, and it's instance.
     *
     * @param rawServer The type of the server.
     * @version 1.1.0
     * @since 3.0.3-SNAPSHOT
     */
    private record ServerMapping(Class<? extends Server> rawServer) implements Mapping {

        /**
         * Retrieves the server instance from {@link CraftsNet}.
         *
         * @return The server instance.
         * @since 3.4.3
         */
        public Function<CraftsNet, Server> server() {
            return craftsNet -> {
                if (this.rawServer.equals(WebServer.class)) return craftsNet.getWebServer();
                if (this.rawServer.equals(WebSocketServer.class)) return craftsNet.getWebSocketServer();
                return null;
            };
        }

        /**
         * Retrieves the server instance from {@link CraftsNet}.
         *
         * @param craftsNet The instance of craftsnet.
         * @return The server instance.
         * @since 3.4.3
         */
        public Server server(CraftsNet craftsNet) {
            return server().apply(craftsNet);
        }

    }

    /**
     * A universal interface for mappings.
     *
     * <p>Provides default methods for checking and retrieving metadata about requirements
     * indexed by annotation types.</p>
     *
     * @version 2.0.0
     * @since 3.0.3-SNAPSHOT
     */
    public interface Mapping {

        /**
         * Checks whether a specific annotation is present in the mapping's requirements.
         *
         * @param annotation The annotation class to check for.
         * @return {@code true} if the annotation is present, {@code false} otherwise.
         */
        default <A extends Annotation> boolean isPresent(@NotNull Class<A> annotation) {
            return false;
        }

        /**
         * Checks whether a specific key within an annotation's requirements is present.
         *
         * @param annotation The annotation class to check for.
         * @param key        The specific key to check within the annotation's requirements.
         * @return {@code true} if the key is present, {@code false} otherwise.
         */
        default <A extends Annotation> boolean isPresent(@NotNull Class<A> annotation, @NotNull String key) {
            return isPresent(annotation);
        }

        /**
         * Retrieves a value associated with a specific annotation's default key ("value").
         *
         * @param annotation The annotation class to retrieve the value from.
         * @return The value associated with the annotation's default key, or {@code null} if not present.
         */
        default <A extends Annotation, T> @Unmodifiable T getRequirements(@NotNull Class<A> annotation) {
            return getRequirements(annotation, "value");
        }

        /**
         * Retrieves a value associated with a specific annotation's requirements key.
         *
         * @param annotation The annotation class to retrieve the value from.
         * @param key        The specific key to retrieve from the annotation's requirements.
         * @return The value associated with the given key, or {@code null} if not present.
         */
        default <A extends Annotation, T> @Unmodifiable T getRequirements(@NotNull Class<A> annotation, @NotNull String key) {
            return null;
        }

    }

}
