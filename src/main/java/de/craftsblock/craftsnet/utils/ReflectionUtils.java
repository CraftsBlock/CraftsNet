package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Some reflection utilities.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.2.0-SNAPSHOT
 */
public class ReflectionUtils {

    /**
     * Checks if a constructor is present in the specified class with the provided argument types.
     *
     * @param clazz The class to check for the constructor.
     * @param args  The argument types to check for.
     * @return true if the constructor is present, false otherwise.
     */
    public static boolean isConstructorPresent(Class<?> clazz, Class<?>... args) {
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
    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... args) {
        try {
            return clazz.getDeclaredConstructor(args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a field in the given object using reflection.
     *
     * @param name The name of the field to set.
     * @param obj  The object whose field needs to be set.
     * @param arg  The value to set the field to.
     * @throws IllegalAccessException if the field cannot be accessed.
     */
    public static void setField(String name, Object obj, Object arg) throws IllegalAccessException {
        Field field = getField(obj.getClass(), name); // Find the specified field in the object's class hierarchy using reflection
        field.setAccessible(true); // Set the fields accessibility to true
        field.set(obj, arg); // Set the field value to the provided argument
        field.setAccessible(false); // Set the fields accessibility to false
    }


    /**
     * Finds a field with the given name in the specified class, including inherited classes.
     *
     * @param clazz The class to search for the field.
     * @param name  The name of the field to find.
     * @return The field if found, otherwise null.
     */
    public static Field getField(Class<?> clazz, String name) {
        Field field = null;
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name); // Attempt to get the specified field from the current class
            } catch (Exception ignored) {
            }

            clazz = clazz.getSuperclass(); // Move to the superclass for further field search
        }
        return field;
    }

    /**
     * Extracts the generic type parameter from a Class.
     *
     * @param clazz The Class to extract the generic type from.
     * @param <T>   The type of handler.
     * @return The class type corresponding to the handler's generic type.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> extractGeneric(Class<?> clazz, Class<?> base) {
        try {
            Type superclass = clazz.getGenericSuperclass();
            if (superclass instanceof ParameterizedType type)
                if (type.getActualTypeArguments().length >= 1) {
                    Type t = type.getActualTypeArguments()[0];
                    if (t instanceof ParameterizedType sub)
                        return (Class<T>) sub.getRawType();
                    return (Class<T>) t;
                }
        } catch (ClassCastException ignored) {
        }

        if (!Object.class.equals(clazz.getSuperclass()) && base.isAssignableFrom(clazz.getSuperclass()))
            return extractGeneric(clazz.getSuperclass(), base);

        return null;
    }

    /**
     * Extracts the generic type {@link T} from a given class.
     *
     * @param clazz The class loader from which to extract the generic type.
     * @param <T>   The type of service handled by the service loader.
     * @return The {@link Class} object representing the generic type {@link T}, or null if unable to extract.
     * @see ParameterizedType
     * @see Class#getGenericInterfaces()
     * @see ParameterizedType#getActualTypeArguments()
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> Class<T> extractGenericInterface(Class<?> clazz) {
        return Arrays.stream(clazz.getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType)
                .map(type -> (ParameterizedType) type)
                .filter(type -> type.getActualTypeArguments().length >= 1)
                .map(type -> (Class<T>) type.getActualTypeArguments()[0])
                .findFirst()
                .orElse(null);
    }

}
