package de.craftsblock.craftsnet.addon.services;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * A simple service loader interface for managing instances of a specified type. This interface defines methods
 * to create a new instance of a class and to load a service provider for further processing.
 *
 * @param <T> The type of service to be loaded.
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.0.0-SNAPSHOT
 */
public interface ServiceLoader<T> {

    /**
     * Creates a new instance of the specified class using its default constructor. This method provides a
     * convenient way to instantiate objects without requiring explicit knowledge of the constructor details.
     *
     * @param clazz The class for which a new instance should be created.
     * @param args  The arguments for creating the new instance.
     * @return A new instance of the specified class.
     * @throws NoSuchMethodException     If the default constructor is not found in the specified class.
     * @throws InvocationTargetException If the constructor invocation fails.
     * @throws InstantiationException    If an instance of the class cannot be created (e.g., if it is an interface or an abstract class).
     * @throws IllegalAccessException    If the default constructor is not accessible due to access restrictions.
     */
    default T newInstance(Class<T> clazz, Object... args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return clazz.getDeclaredConstructor(Arrays.stream(args).map(Object::getClass).toArray(Class[]::new)).newInstance(args);
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
