package de.craftsblock.craftsnet.api.requirements;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementInfo;
import de.craftsblock.craftsnet.api.requirements.meta.RequirementType;
import de.craftsblock.craftsnet.api.requirements.web.*;
import de.craftsblock.craftsnet.api.requirements.websocket.MessageTypeRequirement;
import de.craftsblock.craftsnet.api.requirements.websocket.WSDomainRequirement;
import de.craftsblock.craftsnet.api.requirements.websocket.WebSocketRequirement;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.utils.ReflectionUtils;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * The {@link RequirementRegistry} class manages the registration and unregistration of {@link Requirement}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.2.1-SNAPSHOT
 */
public class RequirementRegistry {

    private final CraftsNet craftsNet;
    private final RouteRegistry routeRegistry;

    private final ConcurrentHashMap<Class<? extends Server>, ConcurrentLinkedQueue<Requirement<? extends RequireAble, RouteRegistry.EndpointMapping>>> requirements = new ConcurrentHashMap<>();

    /**
     * Constructs a new instance of the {@link RequirementRegistry}
     *
     * @param craftsNet The CraftsNet instance which instantiates this requirement registry
     */
    public RequirementRegistry(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
        this.routeRegistry = craftsNet.routeRegistry();

        // Built in http requirements
        register(new BodyRequirement(), false);
        register(new CookieRequirement(), false);
        register(new ContentTypeRequirement(), false);
        register(new HeadersRequirement(), false);
        register(new HTTPDomainRequirement(), false);
        register(new MethodRequirement(), false);
        register(new QueryParameterRequirement(), false);

        // Built in websocket requirements
        register(new MessageTypeRequirement(), false);
        register(new WSDomainRequirement(), false);
    }

    /**
     * Registers and applies a new requirement to the web system.
     *
     * @param requirement The requirement which should be registered.
     */
    public void register(WebRequirement requirement) {
        register(requirement, true);
    }

    /**
     * Registers a new requirement to the web system. Optional the requirement can be applied to all existing
     * endpoints or only to new ones.
     *
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     */
    public void register(WebRequirement requirement, boolean process) {
        if (isRegistered(requirement)) return;
        registerRaw(WebServer.class, requirement, process);
    }

    /**
     * Registers and applies a new requirement to the websocket system.
     *
     * @param requirement The requirement which should be registered.
     */
    public void register(WebSocketRequirement<? extends RequireAble> requirement) {
        register(requirement, true);
    }

    /**
     * Registers a new requirement to the websocket system. Optional the requirement can be applied to all existing
     * endpoints or only to new ones.
     *
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     */
    public void register(WebSocketRequirement<? extends RequireAble> requirement, boolean process) {
        if (isRegistered(requirement)) return;
        registerRaw(WebSocketServer.class, requirement, process);
    }

    /**
     * Registers a new requirement to the targeted server type with the optional feature to reprocess all the already
     * registered endpoints of the server.
     *
     * @param target      The targeted server.
     * @param requirement The requirement which should be registered.
     * @param process     Whether if all registered endpoints should receive the new requirement (true) or not (false).
     */
    private void registerRaw(Class<? extends Server> target, Requirement<? extends RequireAble, RouteRegistry.EndpointMapping> requirement, boolean process) {
        this.requirements.computeIfAbsent(target, c -> new ConcurrentLinkedQueue<>()).add(requirement);
        ConcurrentHashMap<Class<? extends Server>, ConcurrentHashMap<Pattern, ConcurrentLinkedQueue<RouteRegistry.EndpointMapping>>> serverMappings = routeRegistry.getServerMappings();
        if (!process || !serverMappings.containsKey(target)) return;

        ConcurrentHashMap<Pattern, ConcurrentLinkedQueue<RouteRegistry.EndpointMapping>> patternedMappings = serverMappings.get(target);
        if (patternedMappings.isEmpty()) return;

        List<Class<? extends Annotation>> annotations = Collections.singletonList(requirement.getAnnotation());
        patternedMappings.values().stream().flatMap(Collection::stream).forEach(mapping -> {
            ConcurrentHashMap<Class<? extends Annotation>, RequirementInfo> requirements = new ConcurrentHashMap<>();
            loadRequirements(requirements, annotations, mapping.method(), mapping.handler());
            if (requirements.isEmpty()) return;
            mapping.requirements().putAll(requirements);
        });
    }

    /**
     * Checks if the given {@link WebRequirement} is registered.
     * This class is a wrapper for {@link RequirementRegistry#isRegistered(Class)}.
     *
     * @param handler The {@link WebRequirement} to check.
     * @return {@code true} when the {@link WebRequirement} was registered, {@code false} otherwise.
     */
    public boolean isRegistered(WebRequirement handler) {
        return isRegistered(handler.getClass());
    }

    /**
     * Checks if the given {@link WebSocketRequirement} is registered.
     * This class is a wrapper for {@link RequirementRegistry#isRegistered(Class)}.
     *
     * @param handler The {@link WebSocketRequirement} to check.
     * @return {@code true} when the {@link WebSocketRequirement} was registered, {@code false} otherwise.
     */
    @SuppressWarnings("unchecked")
    public boolean isRegistered(WebSocketRequirement<? extends RequireAble> handler) {
        return isRegistered((Class<? extends Requirement<? extends RequireAble, RouteRegistry.EndpointMapping>>) handler.getClass());
    }

    /**
     * Checks if the given class representation of the {@link Requirement} is registered.
     *
     * @param type The class representation of the {@link Requirement} to check.
     * @return {@code true} when the {@link Requirement} was registered, {@code false} otherwise.
     */
    public boolean isRegistered(Class<? extends Requirement<? extends RequireAble, RouteRegistry.EndpointMapping>> type) {
        if (requirements.isEmpty()) return false;

        Class<? extends Server> server;
        if (WebSocketRequirement.class.isAssignableFrom(type)) server = WebSocketServer.class;
        else if (WebRequirement.class.isAssignableFrom(type)) server = WebServer.class;
        else return false;

        if (!requirements.containsKey(server)) return false;
        return requirements.get(server).stream().anyMatch(type::isInstance);
    }

    /**
     * Load all the requirements from a specific list of annotations for a method and its handler.
     *
     * @param requirements A concurrent map where all the processed requirements will be stored.
     * @param annotations  A list of annotation classes to process as requirements.
     * @param method       The method for which the requirements should be loaded.
     * @param handler      The handler object that contains the method and acts as the parent context.
     */
    @ApiStatus.Internal
    public void loadRequirements(ConcurrentHashMap<Class<? extends Annotation>, RequirementInfo> requirements, List<Class<? extends Annotation>> annotations, Method method, Object handler) {
        loadRequirements(requirements, annotations, handler);
        loadRequirements(requirements, annotations, method);
    }

    /**
     * Load all requirements from a specific list of annotations for a given object.
     *
     * @param requirements A concurrent map where all the processed requirements will be stored.
     * @param annotations  A list of annotation classes to process as requirements.
     * @param obj          The object (either a Method or another class instance) to process.
     */
    @ApiStatus.Internal
    private void loadRequirements(ConcurrentHashMap<Class<? extends Annotation>, RequirementInfo> requirements, List<Class<? extends Annotation>> annotations, Object obj) {
        for (Class<? extends Annotation> type : annotations) {
            Annotation annotation = ReflectionUtils.retrieveRawAnnotation(obj, type);
            if (annotation == null) continue;

            RequirementInfo info = new RequirementInfo(annotation);
            if (info.meta().type().equals(RequirementType.STORING) && info.values().isEmpty())
                continue;

            requirements.merge(type, info, RequirementInfo::merge);
        }
    }

    /**
     * Get all registered requirement processors.
     *
     * @return A map which contains all the requirement processors per server sorted.
     */
    public ConcurrentHashMap<Class<? extends Server>, ConcurrentLinkedQueue<Requirement<? extends RequireAble, RouteRegistry.EndpointMapping>>> getRequirements() {
        return new ConcurrentHashMap<>(Collections.unmodifiableMap(requirements));
    }

    /**
     * Get all registered requirement processors for a specific server system.
     *
     * @param server The server system the requirement processors should be loaded from.
     * @return A list which contains all the requirement processors for the specific server system.
     */
    public Collection<Requirement<? extends RequireAble, RouteRegistry.EndpointMapping>> getRequirements(Class<? extends Server> server) {
        return requirements.containsKey(server) ? Collections.unmodifiableCollection(requirements.get(server)) : List.of();
    }

    /**
     * Retrieves the instance of {@link CraftsNet} bound to the registry.
     *
     * @return The instance of {@link CraftsNet} bound to the registry.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

}
