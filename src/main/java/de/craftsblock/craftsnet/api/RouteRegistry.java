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
import de.craftsblock.craftsnet.api.requirements.websocket.MessageTypeRequirement;
import de.craftsblock.craftsnet.api.requirements.websocket.WSDomainRequirement;
import de.craftsblock.craftsnet.api.requirements.websocket.WebSocketRequirement;
import de.craftsblock.craftsnet.api.websocket.*;
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
 * @version 3.0.2
 * @since 1.0.0-SNAPSHOT
 */
public class RouteRegistry {

    private final CraftsNet craftsNet;
    private final Logger logger;

    private final ConcurrentHashMap<Class<? extends Server>, ConcurrentLinkedQueue<Requirement<? extends RequireAble, EndpointMapping>>> requirements = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends Server>, ConcurrentHashMap<Pattern, List<EndpointMapping>>> serverMappings = new ConcurrentHashMap<>();

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
        registerRequirement(new BodyRequirement(), false);
        registerRequirement(new CookieRequirement(), false);
        registerRequirement(new ContentTypeRequirement(), false);
        registerRequirement(new HeadersRequirement(), false);
        registerRequirement(new HTTPDomainRequirement(), false);
        registerRequirement(new MethodRequirement(), false);
        registerRequirement(new QueryParameterRequirement(), false);

        // Built in websocket requirements
        registerRequirement(new MessageTypeRequirement(), false);
        registerRequirement(new WSDomainRequirement(), false);
    }

    /**
     * Registers and applies a new requirement to the web system.
     *
     * @param requirement The requirement which should be registered.
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
     */
    public void registerRequirement(WebRequirement requirement, boolean process) {
        registerRawRequirement(WebServer.class, requirement, process);
    }

    /**
     * Registers and applies a new requirement to the websocket system.
     *
     * @param requirement The requirement which should be registered.
     */
    public void registerRequirement(WebSocketRequirement<? extends RequireAble> requirement) {
        registerRequirement(requirement, true);
    }

    /**
     * Registers a new requirement to the websocket system. Optional the requirement can be applied to all existing
     * endpoints or only to new ones.
     *
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     */
    public void registerRequirement(WebSocketRequirement<? extends RequireAble> requirement, boolean process) {
        registerRawRequirement(WebSocketServer.class, requirement, process);
    }

    /**
     * Get all registered requirement processors.
     *
     * @return A map which contains all the requirement processors per server sorted.
     */
    public ConcurrentHashMap<Class<? extends Server>, ConcurrentLinkedQueue<Requirement<? extends RequireAble, EndpointMapping>>> getRequirements() {
        return new ConcurrentHashMap<>(Map.copyOf(requirements));
    }

    /**
     * Get all registered requirement processors for a specific server system.
     *
     * @param server The server system the requirement processors should be loaded from.
     * @return A list which contains all the requirement processors for the specific server system.
     */
    public Collection<Requirement<? extends RequireAble, EndpointMapping>> getRequirements(Class<? extends Server> server) {
        return requirements.containsKey(server) ? Collections.unmodifiableCollection(requirements.get(server)) : List.of();
    }

    /**
     * Registers a new requirement to the targeted server type with the optional feature to reprocess all the already
     * registered endpoints of the server.
     *
     * @param target      The targeted server.
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     */
    private void registerRawRequirement(Class<? extends Server> target, Requirement<? extends RequireAble, EndpointMapping> requirement, boolean process) {
        this.requirements.computeIfAbsent(target, c -> new ConcurrentLinkedQueue<>()).add(requirement);
        if (!process || !serverMappings.containsKey(target)) return;

        ConcurrentHashMap<Pattern, List<EndpointMapping>> patternedMappings = serverMappings.get(target);
        if (patternedMappings.isEmpty()) return;

        List<Class<? extends Annotation>> annotations = Collections.singletonList(requirement.getAnnotation());
        patternedMappings.forEach((pattern, mappings) -> mappings.forEach(mapping -> {
            ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements = new ConcurrentHashMap<>();

            try {
                loadRequirements(requirements, annotations, mapping.method, mapping.handler);
                requirements.forEach((aClass, objects) -> {
                    if (objects.isEmpty()) requirements.remove(aClass);
                });
                mapping.requirements.putAll(requirements);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    /**
     * Registers an endpoint handler (route or websocket) by inspecting its annotated methods and adding it to the registry.
     *
     * @param handler The Handler to be registered.
     */
    public void register(Handler handler) {
        ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> annotations = retrieveHandlerInfoMap(handler);

        for (Class<? extends Annotation> annotation : annotations.keySet())
            try {
                Class<? extends Server> rawServer = annotations.get(annotation).rawServer();
                ConcurrentHashMap<Pattern, List<EndpointMapping>> endpoints = serverMappings.computeIfAbsent(rawServer, c -> new ConcurrentHashMap<>());
                List<Class<? extends Annotation>> requirementAnnotations = new ArrayList<>(this.requirements.get(rawServer)
                        .parallelStream().map(Requirement::getAnnotation).toList());

                String parent = retrieveValueOfAnnotation(handler, annotation, String.class, true);

                // Continue if no handlers exists for this server type
                if (!Utils.checkForMethodsWithAnnotation(handler.getClass(), annotation)) continue;

                for (Method method : Utils.getMethodsByAnnotation(handler.getClass(), annotation)) {
                    if (WebServer.class.isAssignableFrom(rawServer)) {
                        if (method.getParameterCount() <= 0)
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() + " but does not require " + Exchange.class.getName() + " as the first parameter!");
                        if (!Exchange.class.isAssignableFrom(method.getParameterTypes()[0]))
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() + " but does not require " + Exchange.class.getName() + " as the first parameter!");
                    } else {
                        if (method.getParameterCount() <= 1)
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() + " but does not require " + SocketExchange.class.getName() + " as the first parameter!");
                        if (!SocketExchange.class.isAssignableFrom(method.getParameterTypes()[0]))
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() + " but does not require " + SocketExchange.class.getName() + " as the first parameter!");
                        if (!String.class.isAssignableFrom(method.getParameterTypes()[1]) && !byte[].class.isAssignableFrom(method.getParameterTypes()[1]) && !Frame.class.isAssignableFrom(method.getParameterTypes()[1]))
                            throw new IllegalStateException("The method " + method.getName() + " has the annotation " + annotation.getName() + " but does not require a Frame, String or byte[] as the second parameter!");
                    }

                    String child = retrieveValueOfAnnotation(method, annotation, String.class, true);
                    ProcessPriority priority = retrieveRawAnnotation(method, ProcessPriority.class);
                    Pattern validator = createOrGetValidator(mergeUrl(parent != null ? parent : "", child), endpoints);

                    // Load requirements
                    ConcurrentHashMap<Class<? extends Annotation>, List<Object>> requirements = new ConcurrentHashMap<>();
                    loadRequirements(requirements, requirementAnnotations, method, handler);

                    // Remove empty requirements
                    requirements.forEach((aClass, objects) -> {
                        if (objects.isEmpty()) requirements.remove(aClass);
                    });

                    // Register the endpoint mapping
                    List<EndpointMapping> mappings = endpoints.computeIfAbsent(validator, pattern -> new ArrayList<>());
                    mappings.add(new EndpointMapping(
                            priority != null ? priority.value() : ProcessPriority.Priority.NORMAL,
                            method, handler, validator, requirements
                    ));
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        // Unregister the DefaultRoute
        if (!(handler instanceof DefaultRoute) && (hasRoutes() || hasShares() || hasWebsockets()))
            getRoutes().entrySet().parallelStream()
                    .map(Map.Entry::getValue)
                    .flatMap(Collection::parallelStream)
                    .filter(Objects::nonNull)
                    .filter(mapping -> mapping.handler instanceof DefaultRoute)
                    .map(EndpointMapping::handler)
                    .forEach(this::unregister);

        // Loop through all active servers and turn them on as they are now needed.
        for (ServerMapping mapping : annotations.values())
            if (mapping.server() != null)
                mapping.server().awakeOrWarn();
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
        Pattern pattern = Pattern.compile(formatUrl(path) + "/" + "?(.*)");
        shares.put(pattern, new ShareMapping(folder.getAbsolutePath(), onlyGet));

        // Only continue if the web server was set
        if (this.craftsNet.webServer() != null)
            this.craftsNet.webServer().awakeOrWarn();
    }

    /**
     * Unregisters an endpoint handler (route or websocket) from the registry.
     *
     * @param handler The RequestHandler to be unregistered.
     */
    public void unregister(Handler handler) {
        ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> annotations = retrieveHandlerInfoMap(handler);

        for (Class<? extends Annotation> annotation : annotations.keySet()) {
            try {
                ServerMapping mapping = annotations.get(annotation);

                ConcurrentHashMap<Pattern, List<EndpointMapping>> endpoints = serverMappings.computeIfAbsent(mapping.rawServer(), c -> new ConcurrentHashMap<>());
                String parent = retrieveValueOfAnnotation(handler, annotation, String.class, true);

                // Continue if no handlers exists for this server type
                if (!Utils.checkForMethodsWithAnnotation(handler.getClass(), annotation)) continue;

                for (Method method : Utils.getMethodsByAnnotation(handler.getClass(), annotation)) {
                    String child = retrieveValueOfAnnotation(method, annotation, String.class, true);
                    endpoints.entrySet().removeIf(validator -> validator.getKey().matcher(mergeUrl(parent != null ? parent : "", child)).matches());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        // Loop through all active servers and turn them off if they are not needed.
        for (ServerMapping mapping : annotations.values())
            if (mapping.server() != null)
                mapping.server().sleepIfNotNeeded();
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
    public File getShareFolder(String url) {
        if (!isShare(url)) return null;
        return new File(getShare(url).filepath());
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
    public Map<Pattern, List<EndpointMapping>> getRoutes() {
        return Map.copyOf(serverMappings.computeIfAbsent(WebServer.class, c -> new ConcurrentHashMap<>()));
    }

    /**
     * Gets the route mappings associated with a specific request information.
     *
     * @param request The http request for which a routes should be found.
     * @return A list of RouteMapping objects associated with the URL and HTTP method, or null if no mappings are found.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public List<EndpointMapping> getRoute(Request request) {
        return getRoutes().entrySet().parallelStream()
                .map(Map.Entry::getValue)
                .flatMap(Collection::parallelStream)
                .filter(entry -> entry.validator().matcher(formatUrl(request.getUrl())).matches())
                .filter(entry -> {
                    if (requirements.containsKey(WebServer.class))
                        for (Requirement requirement : requirements.get(WebServer.class)) {
                            boolean applies = requirement.applies(request, entry);
                            if (!applies) {
                                return false;
                            }
                        }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets an immutable copy of the registered socket handlers in the registry.
     *
     * @return A ConcurrentHashMap containing the registered socket handlers.
     */
    @NotNull
    public Map<Pattern, List<EndpointMapping>> getSockets() {
        return Map.copyOf(serverMappings.computeIfAbsent(WebSocketServer.class, c -> new ConcurrentHashMap<>()));
    }

    /**
     * Gets the socket mapping associated with a specific URL and domain.
     *
     * @param client The client for which the socket mapping is sought.
     * @return A list of SocketMapping objects associated with the URL, or null if no mapping is found.
     */
    @Nullable
    public List<EndpointMapping> getSocket(WebSocketClient client) {
        return getSockets().entrySet().parallelStream()
                .map(Map.Entry::getValue)
                .flatMap(Collection::parallelStream)
                .filter(entry -> entry.validator().matcher(formatUrl(client.getPath())).matches())
                .filter(entry -> {
                    if (requirements.containsKey(WebSocketServer.class))
                        for (Requirement requirement : requirements.get(WebSocketServer.class))
                            try {
                                Method method = requirement.getClass().getDeclaredMethod("applies", WebSocketClient.class, EndpointMapping.class);
                                if (!((Boolean) method.invoke(requirement, client, entry))) return false;
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                            }
                    return true;
                })
                .collect(Collectors.toList());
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
        return mappings.keySet().parallelStream()
                .filter(pattern -> pattern.matcher(url).matches())
                .findFirst().orElse(createValidator(url));
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
     */
    @NotNull
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
     */
    @SuppressWarnings("unchecked")
    private <A extends Annotation, T> List<T> loadAnnotationValues(Object obj, Class<A> annotation, Class<T> type) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        List<T> values = new ArrayList<>();
        Object value = retrieveValueOfAnnotation(obj, annotation, type, obj instanceof Method);

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
     */
    private <A extends Annotation> A retrieveRawAnnotation(Object o, Class<A> annotation) {
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
    private <A extends Annotation, T> T retrieveValueOfAnnotation(Object o, Class<A> annotationClass, Class<T> type, boolean fallback) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        A annotation = retrieveRawAnnotation(o, annotationClass);
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
     * Checks if the object can be cast to a targeted type and casts it.
     *
     * @param o    The value which should be cast to the targeted type.
     * @param type The class of the targeted type
     * @param <T>  The targeted type
     * @return Returns the cast value or null if not cast able.
     */
    private static @Nullable <T> T castTo(Object o, Class<T> type) {
        return type.isInstance(o) ? type.cast(o) : null;
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
     * Retrieves a map with information about the server types held by the handler.
     *
     * @param handler The handler from which the information should be retrieved.
     * @return The information about the server types held by the handler.
     */
    private ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> retrieveHandlerInfoMap(Handler handler) {
        ConcurrentHashMap<Class<? extends Annotation>, ServerMapping> annotations = new ConcurrentHashMap<>();
        if (handler instanceof RequestHandler)
            annotations.computeIfAbsent(Route.class, c -> new ServerMapping(WebServer.class, craftsNet.webServer()));

        if (handler instanceof SocketHandler)
            annotations.computeIfAbsent(Socket.class, c -> new ServerMapping(WebSocketServer.class, craftsNet.webSocketServer()));

        if (annotations.isEmpty())
            throw new IllegalStateException("Invalid handler type " + handler.getClass().getSimpleName() + " only RequestHandler and SocketHandler are allowed!");
        return annotations;
    }

    /**
     * The EndpointMapping class represents the mapping of a registered handler.
     * It stores information about the priority, method, handler, validator pattern, and requirements.
     *
     * @version 1.0.0
     * @since 3.0.5-SNAPSHOT
     */
    public record EndpointMapping(@NotNull ProcessPriority.Priority priority, @NotNull Method method, @NotNull Handler handler,
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
    }

    /**
     * The ServerMapping class represents a mapping of a server, and it's instance.
     *
     * @since CraftsNet-3.0.3
     */
    private record ServerMapping(Class<? extends Server> rawServer, Server server) implements Mapping {
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
        default <A extends Annotation, T> List<T> getRequirements(Class<A> annotation, Class<T> type) {
            return List.of();
        }

    }

}
