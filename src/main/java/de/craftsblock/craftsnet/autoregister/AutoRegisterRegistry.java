package de.craftsblock.craftsnet.autoregister;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.meta.Startup;
import de.craftsblock.craftsnet.autoregister.builtin.addons.ServiceLoaderAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.builtin.events.ListenerAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.builtin.http.BodyAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.builtin.http.HandlerAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.builtin.http.StreamEncoderAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.builtin.http.WebRequirementAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.builtin.sockets.WebSocketExtensionAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.builtin.sockets.WebSocketRequirementAutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;
import de.craftsblock.craftsnet.utils.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link AutoRegisterRegistry} is responsible for managing and invoking {@link AutoRegisterHandler} instances
 * based on the provided {@link AutoRegisterInfo}. The registry allows registering and unregistering handlers
 * and ensures that the appropriate handlers are invoked when AutoRegister information is processed.
 *
 * <p>This registry supports handling different classes based on their parent types, invoking handlers that match
 * the parent type of the {@link AutoRegisterInfo}. Handlers are registered using a generic approach and are responsible
 * for performing the actual registration logic.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.2.3
 * @see AutoRegisterInfo
 * @see AutoRegisterHandler
 * @since 3.2.0-SNAPSHOT
 */
public class AutoRegisterRegistry {

    private final CraftsNet craftsNet;
    private final ConcurrentHashMap<Class<?>, AutoRegisterHandler<?>> autoRegisterHandlers = new ConcurrentHashMap<>();

    /**
     * Constructs an {@link AutoRegisterRegistry} that will register default handlers.
     *
     * @param craftsNet The main {@link CraftsNet} instance.
     */
    public AutoRegisterRegistry(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;

        register(new BodyAutoRegisterHandler(craftsNet));
        register(new HandlerAutoRegisterHandler(craftsNet));
        register(new ListenerAutoRegisterHandler(craftsNet));
        register(new ServiceLoaderAutoRegisterHandler(craftsNet));
        register(new StreamEncoderAutoRegisterHandler(craftsNet));
        register(new WebRequirementAutoRegisterHandler(craftsNet));
        register(new WebSocketExtensionAutoRegisterHandler(craftsNet));
        register(new WebSocketRequirementAutoRegisterHandler(craftsNet));
    }

    /**
     * Registers a custom {@link AutoRegisterHandler}. This handler will be used to handle {@link AutoRegisterInfo} of the
     * specified type.
     *
     * @param handler The handler to register.
     * @param <T>     The type of handler to register.
     */
    public <T> void register(AutoRegisterHandler<? extends T> handler) {
        autoRegisterHandlers.put(
                Objects.requireNonNull(ReflectionUtils.extractGeneric(handler.getClass(), AutoRegisterHandler.class, 0)),
                handler
        );
    }

    /**
     * Unregisters an {@link AutoRegisterHandler} by its type.
     *
     * @param type The class type of the handler to unregister.
     * @param <T>  The type of handler.
     * @return The unregistered handler, or null if no handler was registered for this type.
     */
    @SuppressWarnings("unchecked")
    public <T> AutoRegisterHandler<T> unregister(Class<T> type) {
        return (AutoRegisterHandler<T>) autoRegisterHandlers.remove(type);
    }

    /**
     * Unregisters the specified {@link AutoRegisterHandler}.
     *
     * @param handler The handler to unregister.
     * @param <T>     The type of handler.
     * @return true if the handler was unregistered, false if the handler was not found.
     */
    public <T> boolean unregister(AutoRegisterHandler<T> handler) {
        return autoRegisterHandlers.remove(
                ReflectionUtils.extractGeneric(handler.getClass(), AutoRegisterHandler.class, 0),
                handler
        );
    }

    /**
     * Handles a list of {@link AutoRegisterInfo} by delegating to the appropriate handlers.
     * Each {@link AutoRegisterInfo} is passed to the matching handler based on its parent types.
     *
     * @param infos The list of AutoRegisterInfo to handle.
     * @param args  The args that should be applied to the {@link AutoRegisterHandler}.
     */
    public void handleAll(List<AutoRegisterInfo> infos, Object... args) {
        if (infos.isEmpty()) return;
        infos.forEach(info -> handle(info, args));
    }

    /**
     * Handles a single {@link AutoRegisterInfo} by invoking the appropriate handler(s) based on its parent types.
     *
     * @param info The AutoRegisterInfo to handle.
     * @param args The args that should be applied to the {@link AutoRegisterHandler}.
     * @return true if the AutoRegisterInfo was successfully handled, false otherwise.
     */
    public boolean handle(AutoRegisterInfo info, Object... args) {
        // Cancel if no parent types are found
        if (info.getParentTypes().isEmpty()) return false;

        // Pre-check if the auto register info is from @AutoRegister
        if (args.length >= 1)
            if (info.getAnnotation() instanceof AutoRegister annotation && args[0] instanceof Startup startup)
                // Cancel if it was called on the wrong startup.
                if (startup != annotation.startup())
                    return false;

        List<? extends AutoRegisterHandler<?>> handlers = autoRegisterHandlers.entrySet().stream()
                .filter(entry -> info.getParentTypes().contains(entry.getKey().getName()))
                .map(Map.Entry::getValue)
                .toList();

        if (handlers.isEmpty()) {
            craftsNet.logger().warning("Found @AutoRegister on " + info.getClassName() + " but no registry found!");
            return false;
        }

        for (AutoRegisterHandler<?> handler : handlers)
            try {
                Object obj = info.getInstantiated(craftsNet);
                Method method = handler.getClass().getDeclaredMethod("handle",
                        ReflectionUtils.extractGeneric(handler.getClass(), AutoRegisterHandler.class, 0),
                        AutoRegisterInfo.class, Object.class.arrayType());

                method.setAccessible(true);
                boolean result = (boolean) method.invoke(handler, obj, info, args);
                if (result)
                    craftsNet.logger().debug("Auto registered " + info.getClassName() + " with " + handler.getClass().getSimpleName());
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        return true;
    }

}
