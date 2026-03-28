package de.craftsblock.craftsnet.addon.services;

import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A simple service loader interface for managing instances of a specified type. This interface defines methods
 * to create a new instance of a class and to load a service provider for further processing.
 *
 * @param <T> The type of service to be loaded.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.0-SNAPSHOT
 */
public interface ServiceLoader<T> {

    /**
     * Creates a new instance of the specified class using its default constructor. This method provides a
     * convenient way to instantiate objects without requiring explicit knowledge of the constructor details.
     *
     * @param clazz The class for which a new instance should be created.
     * @return A new instance of the specified class.
     * @throws InvocationTargetException If the constructor invocation fails.
     * @throws InstantiationException    If an instance of the class cannot be created (e.g., if it is an interface or an abstract class).
     * @throws IllegalAccessException    If the default constructor is not accessible due to access restrictions.
     */
    default T newInstance(Class<T> clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<T> constructor = ReflectionUtils.findConstructor(clazz);

        if (constructor == null) {
            throw new IllegalStateException("Could not find a non-args constructor for %s".formatted(
                    clazz.getName()
            ));
        }

        return constructor.newInstance();
    }

    /**
     * Loads a service provider and performs necessary operations. This method is intended to be implemented
     * by concrete classes that use this interface to define the specific behavior of loading a service provider.
     *
     * @param provider The instance of the service provider to be loaded.
     * @return true if the provider is successfully loaded, false otherwise.
     */
    boolean load(T provider);

}
