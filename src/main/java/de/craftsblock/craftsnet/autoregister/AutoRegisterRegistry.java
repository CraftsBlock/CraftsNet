package de.craftsblock.craftsnet.autoregister;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
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
 * @version 1.0.0
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
    }

    /**
     * Registers a custom {@link AutoRegisterHandler}. This handler will be used to handle {@link AutoRegisterInfo} of the
     * specified type.
     *
     * @param handler The handler to register.
     * @param <T>     The type of handler to register.
     */
    public <T> void register(AutoRegisterHandler<? extends T> handler) {
        autoRegisterHandlers.put(extractGeneric(handler), handler);
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
        return autoRegisterHandlers.remove(extractGeneric(handler), handler);
    }

    /**
     * Handles a list of {@link AutoRegisterInfo} by delegating to the appropriate handlers.
     * Each {@link AutoRegisterInfo} is passed to the matching handler based on its parent types.
     *
     * @param infos The list of AutoRegisterInfo to handle.
     */
    public void handleAll(List<AutoRegisterInfo> infos) {
        infos.forEach(this::handle);
    }

    /**
     * Handles a single {@link AutoRegisterInfo} by invoking the appropriate handler(s) based on its parent types.
     *
     * @param info The AutoRegisterInfo to handle.
     * @return true if the AutoRegisterInfo was successfully handled, false otherwise.
     */
    public boolean handle(AutoRegisterInfo info) {
        List<? extends AutoRegisterHandler<?>> handlers = autoRegisterHandlers.entrySet().stream()
                .filter(entry -> info.parentTypes().contains(entry.getKey().getName()))
                .map(Map.Entry::getValue)
                .toList();

        if (handlers.isEmpty()) return false;

        Class<?> clazz;
        try {
            clazz = info.loader().loadClass(info.className());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Constructor<?> constructor;
        Object[] args;
        if (constructorPresent(clazz)) {
            constructor = getConstructor(clazz);
            args = new Object[0];
        } else {
            constructor = getConstructor(CraftsNet.class);
            args = new Object[]{craftsNet};
        }

        Object obj;
        try {
            obj = constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        for (AutoRegisterHandler<?> handler : handlers)
            try {
                Method method = handler.getClass().getDeclaredMethod("handle", extractGeneric(handler), Object.class.arrayType());
                method.setAccessible(true);
                method.invoke(handler, obj, args);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        return true;
    }

    /**
     * Extracts the generic type parameter from an {@link AutoRegisterHandler} instance.
     *
     * @param handler The handler to extract the generic type from.
     * @param <T>     The type of the handler.
     * @return The class type corresponding to the handler's generic type.
     */
    private <T> Class<T> extractGeneric(AutoRegisterHandler<T> handler) {
        return extractGeneric(handler.getClass());
    }

    /**
     * Extracts the generic type parameter from a Class representing an {@link AutoRegisterHandler}.
     *
     * @param clazz The Class to extract the generic type from.
     * @param <T>   The type of handler.
     * @return The class type corresponding to the handler's generic type.
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> extractGeneric(Class<?> clazz) {
        try {
            Type superclass = clazz.getGenericSuperclass();
            if (superclass instanceof ParameterizedType type)
                if (type.getActualTypeArguments().length >= 1)
                    return (Class<T>) type.getActualTypeArguments()[0];
        } catch (ClassCastException ignored) {
            ignored.printStackTrace();
        }

        if (!Object.class.equals(clazz.getSuperclass()) && AutoRegisterHandler.class.isAssignableFrom(clazz.getSuperclass()))
            return extractGeneric(clazz.getSuperclass());

        return null;
    }

    /**
     * Checks if a constructor is present in the specified class with the provided argument types.
     *
     * @param clazz The class to check for the constructor.
     * @param args  The argument types to check for.
     * @return true if the constructor is present, false otherwise.
     */
    private boolean constructorPresent(Class<?> clazz, Class<?>... args) {
        try {
            clazz.getDeclaredConstructor(args);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Gets the constructor of the specified class with the provided argument types.
     *
     * @param clazz The class to get the constructor for.
     * @param args  The argument types for the constructor.
     * @return The constructor of the class.
     * @throws RuntimeException If no constructor is found.
     */
    private Constructor getConstructor(Class<?> clazz, Class<?>... args) {
        try {
            return clazz.getDeclaredConstructor(args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
