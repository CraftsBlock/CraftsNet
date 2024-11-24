package de.craftsblock.craftsnet.addon.services;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.builtin.IIOServiceLoader;
import de.craftsblock.craftsnet.addon.services.builtin.handlers.GenericHandlerLoader;
import de.craftsblock.craftsnet.addon.services.builtin.handlers.RequestHandlerLoader;
import de.craftsblock.craftsnet.addon.services.builtin.handlers.SocketHandlerLoader;
import de.craftsblock.craftsnet.addon.services.builtin.listeners.ListenerAdapterLoader;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The ServiceManager class represents a manager responsible for handling various service loaders in a system.
 * It manages the registration, unregistration, and loading of service providers through the use of ServiceLoader instances.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.0.0-SNAPSHOT
 */
public class ServiceManager {

    /**
     * A concurrent map that stores service loaders for different service types.
     * Each service type maps to a queue of service loaders for that type.
     */
    private final ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<ServiceLoader<?>>> providers = new ConcurrentHashMap<>();

    /**
     * Constructs a new ServiceManager and registers default service loaders.
     * Default service loaders can be added during instantiation to provide immediate functionality.
     */
    public ServiceManager(CraftsNet craftsNet) {
        // Register default service loaders
        register(new IIOServiceLoader());

        register(new GenericHandlerLoader(craftsNet));
        register(new RequestHandlerLoader(craftsNet));
        register(new SocketHandlerLoader(craftsNet));

        register(new ListenerAdapterLoader(craftsNet));
    }

    /**
     * Registers a service loader with the ServiceManager.
     *
     * @param loader The service loader to be registered.
     * @param <T>    The type of service handled by the service loader.
     */
    public <T> void register(ServiceLoader<T> loader) {
        Class<T> generic = extractGeneric(loader);
        if (generic == null) return;
        ConcurrentLinkedQueue<ServiceLoader<?>> loaders = providers.computeIfAbsent(generic, c -> new ConcurrentLinkedQueue<>());
        if (loaders.contains(loader)) return;
        loaders.add(loader);
    }

    /**
     * Unregisters a service loader from the ServiceManager.
     *
     * @param loader The service loader to be unregistered.
     * @param <T>    The type of service handled by the service loader.
     */
    public <T> void unregister(ServiceLoader<T> loader) {
        Class<T> generic = extractGeneric(loader);
        if (generic == null) return;
        providers.computeIfAbsent(generic, c -> new ConcurrentLinkedQueue<>()).remove(loader);
    }

    /**
     * Loads a service provider for a specific service type using the provided service loader class.
     *
     * <p>This method attempts to load a service provider for the specified service type {@code spi}
     * using the provided service loader class {@code provider}. It checks for existing providers,
     * including superclass and interface providers, and then initializes and loads the service provider
     * instances through the corresponding service loaders.</p>
     *
     * @param spi      The service type interface/class.
     * @param provider The service loader class responsible for loading the service provider.
     * @param <T>      The type of service handled by the service loader.
     * @return true if the service provider is successfully loaded, false otherwise.
     * @throws RuntimeException If any exception occurs during the instantiation or loading of the service provider.
     *                          This includes NoSuchMethodException, InvocationTargetException, InstantiationException,
     *                          and IllegalAccessException.
     * @see ServiceLoader
     */
    @SuppressWarnings("unchecked")
    public <T> boolean load(Class<T> spi, Class<?> provider) {
        if (!providers.containsKey(spi)) {
            if (spi.getSuperclass() == null) return false;
            AtomicBoolean success = new AtomicBoolean(load(spi.getSuperclass(), provider));
            for (Class<?> iface : spi.getInterfaces())
                if (load(iface, provider)) success.set(true);
            return success.get();
        }

        List<ServiceLoader<T>> loaders = providers.get(spi).stream()
                .filter(loader -> {
                    Class<T> loaderClass = (Class<T>) extractGeneric(loader);
                    return loaderClass != null && loaderClass.isAssignableFrom(provider);
                })
                .map(loader -> (ServiceLoader<T>) loader)
                .toList();
        if (loaders.isEmpty()) return false;

        AtomicBoolean success = new AtomicBoolean(false);
        loaders.forEach(loader -> {
            try {
                T instance = loader.newInstance((Class<T>) provider);
                if (loader.load(instance)) success.set(true);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        return success.get();
    }

    /**
     * Extracts the generic type {@code T} from a given service loader instance.
     *
     * <p>This method analyzes the generic interfaces implemented by the provided service loader
     * to extract and return the specific generic type {@code T}. It utilizes Java Reflection to inspect
     * the type information and perform the extraction.</p>
     *
     * @param loader The service loader from which to extract the generic type.
     * @param <T>    The type of service handled by the service loader.
     * @return The {@code Class} object representing the generic type {@code T}, or null if unable to extract.
     * @apiNote The method uses stream operations to filter and map the generic interfaces to find
     * the actual type argument representing {@code T}. It returns null if no suitable type argument is found.
     * @see ServiceLoader
     * @see ParameterizedType
     * @see Class#getGenericInterfaces()
     * @see ParameterizedType#getActualTypeArguments()
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private <T> Class<T> extractGeneric(ServiceLoader<T> loader) {
        return Arrays.stream(loader.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType)
                .map(type -> (ParameterizedType) type)
                .filter(type -> type.getActualTypeArguments().length >= 1)
                .map(type -> (Class<T>) type.getActualTypeArguments()[0])
                .findFirst()
                .orElse(null);
    }

}
