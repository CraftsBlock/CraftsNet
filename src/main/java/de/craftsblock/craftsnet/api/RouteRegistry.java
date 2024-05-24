package de.craftsblock.craftsnet.api;

import de.craftsblock.craftscore.utils.Utils;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.http.*;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.api.http.builtin.DefaultRoute;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.requirements.Requirement;
import de.craftsblock.craftsnet.api.requirements.web.*;
import de.craftsblock.craftsnet.api.requirements.websocket.WSDomainRequirement;
import de.craftsblock.craftsnet.api.requirements.websocket.WebSocketRequirement;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import de.craftsblock.craftsnet.api.websocket.SocketHandler;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.api.websocket.annotations.MessageReceiver;
import de.craftsblock.craftsnet.api.websocket.annotations.Socket;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The RouteRegistry class manages the registration and unregistration of request handlers (routes) and socket handlers.
 * It stores and maps the registered routes and sockets based on their patterns, allowing for efficient handling of incoming requests.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 3.0.0
 * @since CraftsNet1.0.0
 */
public class RouteRegistry {

    private final CraftsNet craftsNet;
    private final Logger logger;

    private final ConcurrentHashMap<Class<? extends Server>, ConcurrentLinkedQueue<Requirement<? extends RequireAble, ? extends Mapping>>> requirements = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends Server>, ConcurrentHashMap<Pattern, List<Mapping>>> serverMappings = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Pattern, ShareMapping> shares = new ConcurrentHashMap<>();

    /**
     * Constructs a new instance of the RouteRegistry
     *
     * @param craftsNet The CraftsNet instance which instantiates this route registry
     */
    public RouteRegistry(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.logger();

        // Built in http requirements
        registerRequirement(new HTTPDomainRequirement());
        registerRequirement(new MethodRequirement());
        registerRequirement(new HeadersRequirement());
        registerRequirement(new ContentTypeRequirement());
        registerRequirement(new BodyRequirement());

        // Built in websocket requirements
        registerRequirement(new WSDomainRequirement());
    }

    /**
     * Registers and applies a new requirement to the web system.
     *
     * @param requirement The requirement which should be registered.
     * @since 3.0.5-SNAPSHOT
     */
    public void registerRequirement(WebRequirement requirement) {
        registerRequirement(requirement, true);
    }

    /**
     * Registers a new requirement to the web system. Optional the requirement can be applied to all existing
     * endpoints or only to new ones.
     *
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     * @since 3.0.5-SNAPSHOT
     */
    public void registerRequirement(WebRequirement requirement, boolean process) {
        registerRawRequirement(WebServer.class, requirement, process);
    }

    /**
     * Registers and applies a new requirement to the websocket system.
     *
     * @param requirement The requirement which should be registered.
     * @since 3.0.5-SNAPSHOT
     */
    public void registerRequirement(WebSocketRequirement requirement) {
        registerRequirement(requirement, true);
    }

    /**
     * Registers a new requirement to the websocket system. Optional the requirement can be applied to all existing
     * endpoints or only to new ones.
     *
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     * @since 3.0.5-SNAPSHOT
     */
    public void registerRequirement(WebSocketRequirement requirement, boolean process) {
        registerRawRequirement(WebSocketServer.class, requirement, process);
    }

    /**
     * Registers a new requirement to the targeted server type with the optional feature to reprocess all the already
     * registered endpoints of the server.
     *
     * @param target      The targeted server.
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     * @since 3.0.5-SNAPSHOT
     */
    private void registerRawRequirement(Class<? extends Server> target, Requirement<? extends RequireAble, ? extends Mapping> requirement, boolean process) {
        this.requirements.computeIfAbsent(target, c -> new ConcurrentLinkedQueue<>()).add(requirement);
        if (!process || !serverMappings.containsKey(target)) return;

        ConcurrentHashMap<Pattern, List<Mapping>> patternedMappings = serverMappings.get(target);
        if (patternedMappings.isEmpty()) return;

        List<Class<? extends Annotation>> annotations = Collections.singletonList(requirement.getAnnotation());
        patternedMappings.forEach((pattern, mappings) -> mappings.forEach(mapping -> {
            ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements = new ConcurrentHashMap<>();

            try {
                if (mapping instanceof RouteMapping routeMapping) {
                    loadRequirements(requirements, annotations, routeMapping.method, routeMapping.handler);
                    requirements.forEach((aClass, objects) -> {
                        if (objects.isEmpty()) requirements.remove(aClass);
                    });
                    routeMapping.requirements.putAll(requirements);
                }
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    /**
     * Registers a request handler (route) by inspecting its annotated methods and adding it to the registry.
     * The method should have Exchange as its first argument and be annotated with {@link Route}.
     *
     * @param handler The RequestHandler to be registered.
     * @since CraftsNet-1.0.0
     */
    public void register(RequestHandler handler) {
        ConcurrentHashMap<Pattern, List<Mapping>> routes = serverMappings.computeIfAbsent(WebServer.class, c -> new ConcurrentHashMap<>());
        List<Class<? extends Annotation>> annotations = new ArrayList<>(this.requirements.get(WebServer.class)
                .parallelStream().map(Requirement::getAnnotation).toList());

        Route parent = rawAnnotation(handler, Route.class);

        for (Method method : Utils.getMethodsByAnnotation(handler.getClass(), Route.class))
            try {
                if (method.getParameterCount() <= 0)
                    throw new IllegalStateException("The methode " + method.getName() + " has the annotation " + Route.class.getName() + " but does not require " + Exchange.class.getName() + " as the first parameter!");
                if (!Exchange.class.isAssignableFrom(method.getParameterTypes()[0]))
                    throw new IllegalStateException("The methode " + method.getName() + " has the annotation " + Route.class.getName() + " but does not require " + Exchange.class.getName() + " as the first parameter!");

                Route route = rawAnnotation(method, Route.class);
                ProcessPriority priority = rawAnnotation(method, ProcessPriority.class);
                Pattern validator = createOrGetValidator(url(parent != null ? parent.value() : "", route.value()), routes);

                // Load requirements
                ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements = new ConcurrentHashMap<>();
                loadRequirements(requirements, annotations, method, handler);

                // Remove empty requirements
                requirements.forEach((aClass, objects) -> {
                    if (objects.isEmpty()) requirements.remove(aClass);
                });

                // Register the route mapping
                List<Mapping> mappings = routes.computeIfAbsent(validator, pattern -> new ArrayList<>());
                mappings.add(new RouteMapping(
                        priority != null ? priority.value() : ProcessPriority.Priority.NORMAL,
                        method, handler, validator, requirements
                ));
            } catch (Exception e) {
                logger.error(e);
            }

        // Unregister the DefaultRoute
        if (!(handler instanceof DefaultRoute))
            routes.entrySet().parallelStream()
                    .map(Map.Entry::getValue)
                    .flatMap(Collection::parallelStream)
                    .filter(RouteMapping.class::isInstance)
                    .map(RouteMapping.class::cast)
                    .filter(mapping -> mapping.handler instanceof DefaultRoute)
                    .map(RouteMapping::handler)
                    .forEach(this::unregister);

        // Only continue if the web server was set
        if (this.craftsNet.webServer() != null)
            this.craftsNet.webServer().awakeOrWarn();
    }

    /**
     * Registers a socket handler by inspecting its annotated methods and adding it to the registry.
     * The method should be annotated with {@link Socket}.
     *
     * @param handler The SocketHandler to be registered.
     * @param <T>     The type of the SocketHandler.
     * @since CraftsNet-2.1.1
     */
    public <T extends SocketHandler> void register(T handler) {
        ConcurrentHashMap<Pattern, List<Mapping>> sockets = serverMappings.computeIfAbsent(WebSocketServer.class, c -> new ConcurrentHashMap<>());
        List<Class<? extends Annotation>> annotations = new ArrayList<>(this.requirements.get(WebSocketServer.class)
                .parallelStream().map(Requirement::getAnnotation).toList());

        Socket parent = rawAnnotation(handler, Socket.class);

        // Used for backwards compatibility
        // For removal in version 4.0.0
        List<Method> outdatedMethods = Utils.getMethodsByAnnotation(handler.getClass(), MessageReceiver.class);
        if (!outdatedMethods.isEmpty()) {
            logger.warning("Found" + (outdatedMethods.size() == 1 ? "" : " " + outdatedMethods.size()) +
                    " outdated websocket creation in class " + handler.getClass().getSimpleName());

            try {
                Socket socket = rawAnnotation(handler, Socket.class);
                Pattern validator = createOrGetValidator(socket.value(), sockets);

                // Load requirements
                ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements = new ConcurrentHashMap<>();
                loadRequirements(requirements, annotations, null, handler);

                // Remove empty requirements
                requirements.forEach((aClass, objects) -> {
                    if (objects.isEmpty()) requirements.remove(aClass);
                });

                List<Mapping> mappings = sockets.computeIfAbsent(validator, pattern -> new ArrayList<>());
                for (Method method : outdatedMethods)
                    mappings.add(new SocketMapping(ProcessPriority.Priority.NORMAL, method, handler, validator, requirements));
            } catch (Exception e) {
                logger.error(e);
            }
            return;
        } else
            // New method of loading websockets
            for (Method method : Utils.getMethodsByAnnotation(handler.getClass(), Socket.class))
                try {
                    if (method.getParameterCount() <= 1)
                        throw new IllegalStateException("The methode " + method.getName() + " has the annotation " + Socket.class.getName() + " but does not require " + SocketExchange.class.getName() + " as the first parameter!");
                    if (!SocketExchange.class.isAssignableFrom(method.getParameterTypes()[0]))
                        throw new IllegalStateException("The methode " + method.getName() + " has the annotation " + Socket.class.getName() + " but does not require " + SocketExchange.class.getName() + " as the first parameter!");
                    if (!String.class.isAssignableFrom(method.getParameterTypes()[1]) && !byte[].class.isAssignableFrom(method.getParameterTypes()[1]))
                        throw new IllegalStateException("The methode " + method.getName() + " has the annotation " + Socket.class.getName() + " but does not require a String or byte[] as the second parameter!");

                    Socket socket = rawAnnotation(method, Socket.class);
                    ProcessPriority priority = rawAnnotation(method, ProcessPriority.class);
                    Pattern validator = createOrGetValidator(url(parent != null ? parent.value() : "", socket.value()), sockets);

                    // Load requirements
                    ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements = new ConcurrentHashMap<>();
                    loadRequirements(requirements, annotations, null, handler);

                    // Remove empty requirements
                    requirements.forEach((aClass, objects) -> {
                        if (objects.isEmpty()) requirements.remove(aClass);
                    });

                    List<Mapping> mappings = serverMappings.computeIfAbsent(WebSocketServer.class, c -> new ConcurrentHashMap<>())
                            .computeIfAbsent(validator, pattern -> new ArrayList<>());
                    mappings.add(new SocketMapping(
                            priority != null ? priority.value() : ProcessPriority.Priority.NORMAL,
                            method, handler, validator, requirements
                    ));
                } catch (Exception e) {
                    logger.error(e);
                }

        // Only continue if the web socket server was set
        if (this.craftsNet.webSocketServer() != null)
            this.craftsNet.webSocketServer().awakeOrWarn();
    }

    /**
     * Shares a folder for a specified path.
     *
     * @param path   The path pattern to share.
     * @param folder The folder to be shared.
     * @throws IllegalArgumentException If the provided "folder" is not a directory.
     */
    public void share(String path, File folder) {
        share(path, folder, true);
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
        if (!folder.isDirectory())
            throw new IllegalArgumentException("\"folder\" must be a folder!");
        Pattern pattern = Pattern.compile(url(path) + "/" + "?(.*)");
        shares.put(pattern, new ShareMapping(folder.getAbsolutePath(), onlyGet));

        // Only continue if the web server was set
        if (this.craftsNet.webServer() != null)
            this.craftsNet.webServer().awakeOrWarn();
    }

    /**
     * Unregisters a request handler (route) from the registry.
     *
     * @param handler The RequestHandler to be unregistered.
     * @since CraftsNet-1.0.0
     */
    public void unregister(RequestHandler handler) {
        ConcurrentHashMap<Pattern, List<Mapping>> routes = serverMappings.computeIfAbsent(WebServer.class, c -> new ConcurrentHashMap<>());
        Route parent = rawAnnotation(handler, Route.class);
        for (Method method : Utils.getMethodsByAnnotation(handler.getClass(), Route.class))
            try {
                Route route = rawAnnotation(method, Route.class);
                routes.entrySet().removeIf(validator -> validator.getKey().matcher(url(parent != null ? parent.value() : "", route.value())).matches());
            } catch (Exception e) {
                logger.error(e);
            }

        // Only continue if the web server has been set up
        if (this.craftsNet.webServer() != null)
            this.craftsNet.webServer().sleepIfNotNeeded();
    }

    /**
     * Unregisters a socket handler from the registry.
     *
     * @param t   The SocketHandler to be unregistered.
     * @param <T> The type of the SocketHandler.
     * @since CraftsNet-2.1.1
     */
    public <T extends SocketHandler> void unregister(T t) {
        Socket socket = rawAnnotation(t, Socket.class);
        getSockets().entrySet().removeIf(validator -> validator.getKey().matcher(url(socket.value())).matches());

        // Only continue if the web server has been set up
        if (this.craftsNet.webSocketServer() != null)
            this.craftsNet.webSocketServer().sleepIfNotNeeded();
    }

    /**
     * Returns an immutable copy of the shared folders and patterns.
     *
     * @return An immutable copy of the shared folders and patterns.
     * @since CraftsNet-2.3.2
     */
    public ConcurrentHashMap<Pattern, File> getShares() {
        ConcurrentHashMap<Pattern, File> result = new ConcurrentHashMap<>();
        shares.forEach((pattern, mapping) -> result.put(pattern, new File(mapping.filepath())));
        return result;
    }

    /**
     * Gets the mapping for the share associated with the given URL.
     *
     * @param url The URL for which to retrieve the associated mapping.
     * @return The mapping of the share or null if no match is found.
     * @since CraftsNet-2.3.2
     */
    public ShareMapping getShare(String url) {
        return shares.entrySet().parallelStream()
                .filter(entry -> entry.getKey().matcher(url(url)).matches())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the shared folder associated with the given URL.
     *
     * @param url The URL for which to retrieve the associated shared folder.
     * @return The shared folder or null if no match is found.
     * @since CraftsNet-2.3.2
     */
    public File getShareFolder(String url) {
        if (!isShare(url)) return null;
        return new File(getShare(url).filepath());
    }

    /**
     * Gets the pattern associated with the shared folder that matches the given URL.
     *
     * @param url The URL for which to retrieve the associated pattern.
     * @return The pattern or null if no match is found.
     * @since CraftsNet-2.3.2
     */
    public Pattern getSharePattern(String url) {
        return shares.keySet().parallelStream()
                .filter(file -> file.matcher(url(url)).matches())
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a URL corresponds to a shared folder by verifying if both a shared folder and its pattern exist for the URL.
     *
     * @param url The URL to check.
     * @return True if the URL corresponds to a shared folder, false otherwise.
     * @since CraftsNet-2.3.2
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
     * @since CraftsNet-1.0.0
     */
    @NotNull
    public ConcurrentHashMap<Pattern, List<RouteMapping>> getRoutes() {
        return getMappingsOfType(WebServer.class, RouteMapping.class);
    }

    /**
     * Gets the route mappings associated with a specific request information.
     *
     * @param request The http request for which a routes should be found.
     * @return A list of RouteMapping objects associated with the URL and HTTP method, or null if no mappings are found.
     * @since CraftsNet-2.3.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public List<RouteMapping> getRoute(Request request) {
        return getRoutes().entrySet().parallelStream()
                .map(Map.Entry::getValue)
                .flatMap(Collection::parallelStream)
                .filter(entry -> {
                    if (requirements.containsKey(WebServer.class))
                        for (Requirement requirement : requirements.get(WebServer.class))
                            if (!requirement.applies(request, entry)) return false;

                    return entry.validator().matcher(url(request.getUrl())).matches();
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets an immutable copy of the registered socket handlers in the registry.
     *
     * @return A ConcurrentHashMap containing the registered socket handlers.
     * @since CraftsNet-2.1.1
     */
    @NotNull
    public ConcurrentHashMap<Pattern, List<SocketMapping>> getSockets() {
        return getMappingsOfType(WebSocketServer.class, SocketMapping.class);
    }

    /**
     * Gets the socket mapping associated with a specific URL and domain.
     *
     * @param client The client for which the socket mapping is sought.
     * @return A list of SocketMapping objects associated with the URL, or null if no mapping is found.
     * @since CraftsNet-2.1.1
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public List<SocketMapping> getSocket(WebSocketClient client) {
        return getSockets().entrySet().parallelStream()
                .map(Map.Entry::getValue)
                .flatMap(Collection::parallelStream)
                .filter(entry -> {
                    if (requirements.containsKey(WebSocketServer.class))
                        for (Requirement requirement : requirements.get(WebSocketServer.class))
                            if (!requirement.applies(client, entry)) return false;

                    return entry.validator().matcher(url(client.getPath())).matches();
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates a validator pattern for a given URL.
     *
     * @param url The URL for which the validator pattern is created.
     * @return The Pattern object representing the validator pattern.
     * @since CraftsNet-1.0.0
     */
    @NotNull
    private Pattern createValidator(String url) {
        Pattern pattern = Pattern.compile("\\{(.*?[^/]+)\\}", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(url(url));
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
        Pattern created = createValidator(url);
        return mappings.keySet().parallelStream()
                .filter(pattern -> pattern.pattern().equalsIgnoreCase(created.pattern()))
                .findFirst().orElse(created);
    }

    /**
     * Load all the requirements from a specific list of annotations
     *
     * @param requirements A list where all the requirements should go.
     * @param annotations  A list with all requirements as their annotation representations.
     * @param method       The method for which the requirements should be loaded.
     * @param handler      The handler which contains the method and acts as the parent.
     * @throws InvocationTargetException If the attribute's getter method is not found.
     * @throws NoSuchMethodException     If there is an issue invoking the getter method.
     * @throws IllegalAccessException    If there is an access issue with the getter method.
     * @since CraftsNet-3.0.5
     */
    private void loadRequirements(ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements, List<Class<? extends Annotation>> annotations, Method method, Object handler) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        for (Class<? extends Annotation> annotation : annotations) {
            Class<?> type = annotationMethodType(annotation, "value");
            if (type == null) continue;

            List<Object> requirement = requirements.computeIfAbsent(annotation, aClass -> new ArrayList<>());
            List<?> values = loadAnnotationValues(method, handler, annotation, type);

            if (type.isArray()) {
                values.forEach(o -> requirement.addAll(List.of((Object[]) o)));
                continue;
            }

            requirement.addAll(values);
        }
    }

    /**
     * Loads all values of the annotation from the parent class and targeted method.
     *
     * @param m          The targeted method.
     * @param obj        The parent class.
     * @param annotation The class of the annotation.
     * @param type       The class of the targeted type.
     * @param <A>        The type of the annotation.
     * @param <T>        The type of the attribute value.
     * @return A list with all the values of the parent and the method.
     * @throws NoSuchMethodException     If the attribute's getter method is not found.
     * @throws InvocationTargetException If there is an issue invoking the getter method.
     * @throws IllegalAccessException    If there is an access issue with the getter method.
     * @since CraftsNet-3.0.4
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private <A extends Annotation, T> List<T> loadAnnotationValues(Method m, Object obj, Class<A> annotation, Class<T> type) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        List<T> values = new ArrayList<>();

        if (obj != null) values.addAll(loadAnnotationValues(obj, annotation, type));
        if (m != null) values.addAll(loadAnnotationValues(m, annotation, type));

        // Remove duplicates
        removeDuplicates(values);
        return values;
    }

    /**
     * Loads all values of the annotation from the parent class or the targeted method.
     *
     * @param obj        The parent class or the method.
     * @param annotation The class of the annotation.
     * @param type       The class of the targeted type.
     * @param <A>        The type of the annotation.
     * @param <T>        The type of the attribute value.
     * @return A list with all the values of the parent and the method.
     * @throws NoSuchMethodException     If the attribute's getter method is not found.
     * @throws InvocationTargetException If there is an issue invoking the getter method.
     * @throws IllegalAccessException    If there is an access issue with the getter method.
     * @since CraftsNet-3.0.5
     */
    @SuppressWarnings("unchecked")
    private <A extends Annotation, T> List<T> loadAnnotationValues(Object obj, Class<A> annotation, Class<T> type) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        List<T> values = new ArrayList<>();
        Object value = annotation(obj, annotation, type, obj instanceof Method);

        if (value != null) {
            Class<?> clazz = value.getClass();

            if (value instanceof List<?> && List.class.isAssignableFrom(type)) values.addAll((Collection<? extends T>) value);
            else if (value instanceof Object[] array && type.isArray()) addArray(array, values, type);
            else if (type.isInstance(value)) values.add(type.cast(value));
            else logger.warning("Found no suitable type of annotation. " +
                        "Found: " + clazz.getSimpleName() + (clazz.isArray() ? "[]" : "") + " " +
                        "Expected: " + type.getSimpleName() + (type.isArray() ? "[]" : ""));
        }

        return values;
    }

    /**
     * This method searches for an annotation of the specified type in an object.
     *
     * @param o          The object in which to search for the annotation.
     * @param annotation The class of the annotation to search for.
     * @param <A>        The type of the annotation.
     * @return The found annotation or null if none is found.
     * @since CraftsNet-2.3.0
     */
    private <A extends Annotation> A rawAnnotation(Object o, Class<A> annotation) {
        if (o instanceof Method method) return method.getAnnotation(annotation);
        return o.getClass().getAnnotation(annotation);
    }

    /**
     * Retrieves the value of a specified annotation attribute from an object.
     *
     * @param <A>             The type of the annotation.
     * @param <T>             The type of the attribute value.
     * @param o               The object containing the annotation.
     * @param annotationClass The class of the annotation.
     * @param type            The class of the targeted type.
     * @param fallback        Defines if the default value should be returned.
     * @return The value of the specified annotation attribute.
     * @throws NoSuchMethodException     If the attribute's getter method is not found.
     * @throws InvocationTargetException If there is an issue invoking the getter method.
     * @throws IllegalAccessException    If there is an access issue with the getter method.
     */
    private <A extends Annotation, T> T annotation(Object o, Class<A> annotationClass, Class<T> type, boolean fallback) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        A annotation = rawAnnotation(o, annotationClass);
        if (annotation == null)
            if (fallback) {
                Method method = annotationClass.getDeclaredMethod("value");
                if (method.getDefaultValue() == null) return null;
                return castTo(method.getDefaultValue(), type);
            } else return null;

        Method method = annotation.getClass().getDeclaredMethod("value");
        Object value = method.invoke(annotation);
        if (value == null)
            if (fallback) value = method.getDefaultValue();
            else return null;

        return castTo(value, type);
    }

    /**
     * Returns the return type as it's class representation of a specific method of an annotation.
     *
     * @param annotation The class representation of the annotation.
     * @param method     The name of the method
     * @param <A>        The type of the annotation.
     * @return The class representation of the return type.
     * @since CraftsNet-3.0.4
     */
    private <A extends Annotation> Class<?> annotationMethodType(Class<A> annotation, String method) {
        try {
            if (annotation == null) return null;
            return annotation.getDeclaredMethod(method).getReturnType();
        } catch (NoSuchMethodException ignored) {
        }
        return null;
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
        result += child;

        return url(result);
    }

    /**
     * Checks if the object can be cast to a targeted type and casts it.
     *
     * @param o    The value which should be cast to the targeted type.
     * @param type The class of the targeted type
     * @param <T>  The targeted type
     * @return Returns the cast value or null if not cast able.
     * @since CraftsNet-3.0.4
     */
    private static @Nullable <T> T castTo(Object o, Class<T> type) {
        return type.isInstance(o) ? type.cast(o) : null;
    }

    /**
     * Checks if the registry has any registered socket handlers.
     *
     * @return true if the registry has registered socket handlers, false otherwise.
     * @since CraftsNet-2.1.1
     */
    public boolean hasWebsockets() {
        return !getSockets().isEmpty();
    }

    /**
     * Checks if the registry has any registered route handlers or shares.
     *
     * @return true if the registry has registered route handlers or shares, false otherwise.
     * @since CraftsNet-2.1.1
     */
    public boolean hasRoutes() {
        return !getRoutes().isEmpty() || hasShares();
    }

    /**
     * Checks if the registry has any registered shares.
     *
     * @return true if the registry has registered shares, false otherwise.
     * @since CraftsNet-2.3.2
     */
    public boolean hasShares() {
        return !shares.isEmpty();
    }

    /**
     * Formats the URL by ensuring it starts and does not end with a slash.
     * It also removes duplicate slashes from the url.
     *
     * @param url The URL to be formatted.
     * @return The formatted URL.
     * @since CraftsNet-1.0.0
     */
    private String url(String url) {
        String result = (!url.trim().startsWith("/") ? "/" : "") + url.trim();
        return result
                .replaceAll("/+", "/")
                .replaceAll("/+$", "");
    }

    /**
     * Removes duplicates from a list by converting it to a set and then back to a list.
     *
     * @param <T>  The type of elements in the list.
     * @param list The list from which duplicates will be removed.
     */
    private <T> void removeDuplicates(List<T> list) {
        HashSet<T> unique = new HashSet<>(list);
        list.clear();
        list.addAll(unique);
    }

    /**
     * Adds elements from an array to a list.
     *
     * @param <T>  The type of elements in the array and list.
     * @param t    The array containing elements to add.
     * @param list The list to which elements will be added.
     */
    private <T> void addArray(Object[] t, List<T> list, Class<T> type) {
        if (!type.isInstance(t)) return;
        list.add(type.cast(t));
    }

    /**
     * Loads the list of mappings from the specific server system and cast the values to the required type.
     *
     * @param server The server system from which the mappings should be loaded from.
     * @param type   The targeted type to which all the mappings should be cast.
     * @param <S>    The type of the server system.
     * @param <T>    The type of the targeted result type.
     * @return The list of mappings.
     * @since 3.0.5
     */
    private <S extends Server, T extends Mapping> ConcurrentHashMap<Pattern, List<T>> getMappingsOfType(Class<S> server, Class<T> type) {
        return new ConcurrentHashMap<>(Map.copyOf(
                serverMappings.computeIfAbsent(server, c -> new ConcurrentHashMap<>()).entrySet().parallelStream()
                        .filter(entry -> entry.getValue().parallelStream().allMatch(type::isInstance))
                        .map(entry -> Map.entry(entry.getKey(), entry.getValue().parallelStream().map(type::cast).toList()))
                        .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue))
        ));
    }

    /**
     * The RouteMapping class represents the mapping of a registered route.
     * It stores information about the method, handler, and validator pattern for the route.
     *
     * @version 1.1
     * @since CraftsNet-1.0.0
     */
    public record RouteMapping(@NotNull ProcessPriority.Priority priority, @NotNull Method method, @NotNull RequestHandler handler,
                               @NotNull Pattern validator,
                               ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements) implements Mapping {

        /**
         * {@inheritDoc}
         *
         * @param annotation The annotation as it's class representation used to find the requirements.
         * @param type       The expected return type as it's class representation.
         * @param <A>        The annotation used to find the requirements.
         * @param <T>        The expected return type.
         * @return A list of requirements which are listed under the annotation.
         */
        @Override
        public <A extends Annotation, T> List<T> getRequirements(Class<A> annotation, Class<T> type) {
            if (!requirements.containsKey(annotation)) return null;
            List<Object> requirement = requirements.get(annotation);
            return requirement.parallelStream().filter(type::isInstance).map(type::cast).toList();
        }

    }

    /**
     * The SocketMapping class represents the mapping of a registered socket handler.
     * It stores information about the socket handler, socket annotation, validator pattern, and message receiver methods.
     *
     * @since CraftsNet-2.1.1
     */
    public record SocketMapping(@NotNull ProcessPriority.Priority priority, @NotNull Method method, @NotNull SocketHandler handler,
                                @NotNull Pattern validator,
                                ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements) implements Mapping {

        /**
         * {@inheritDoc}
         *
         * @param annotation The annotation as it's class representation used to find the requirements.
         * @param type       The expected return type as it's class representation.
         * @param <A>        The annotation used to find the requirements.
         * @param <T>        The expected return type.
         * @return A list of requirements which are listed under the annotation.
         */
        @Override
        public <A extends Annotation, T> List<T> getRequirements(Class<A> annotation, Class<T> type) {
            if (!requirements.containsKey(annotation)) return null;
            List<Object> requirement = requirements.get(annotation);
            return requirement.parallelStream().filter(type::isInstance).map(type::cast).toList();
        }

    }

    /**
     * The ShareMapping class represents the mapping of a registered shared endpoint.
     * It stores information about the filesystem path and if only get requests should be processed.
     *
     * @since CraftsNet-3.0.3
     */
    public record ShareMapping(@NotNull String filepath, boolean onlyGet) implements Mapping {

        /**
         * {@inheritDoc}
         *
         * @param annotation The annotation as it's class representation used to find the requirements.
         * @param type       The expected return type as it's class representation.
         * @param <A>        The annotation used to find the requirements.
         * @param <T>        The expected return type.
         * @return A list of requirements which are listed under the annotation.
         */
        @Override
        public <A extends Annotation, T> List<T> getRequirements(Class<A> annotation, Class<T> type) {
            return List.of();
        }

    }

    /**
     * A universal interface for mappings.
     *
     * @since CraftsNet-3.0.3
     */
    public interface Mapping {

        /**
         * Retrieves a list of requirements which were registered under a certain annotation class.
         *
         * @param annotation The annotation as it's class representation used to find the requirements.
         * @param type       The expected return type as it's class representation.
         * @param <A>        The annotation used to find the requirements.
         * @param <T>        The expected return type.
         * @return A list of requirements which are listed under the annotation.
         */
        <A extends Annotation, T> List<T> getRequirements(Class<A> annotation, Class<T> type);

    }

}
