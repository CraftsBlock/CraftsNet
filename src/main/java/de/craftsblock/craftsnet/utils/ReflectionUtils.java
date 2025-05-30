package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;

/**
 * Some reflection utilities.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.2.0
 * @since 3.2.0-SNAPSHOT
 */
public class ReflectionUtils {

    /**
     * Private constructor to prevent direct instantiation
     */
    private ReflectionUtils() {
    }

    /**
     * Retrieves the class of the immediate caller of the method in which this method is called.
     *
     * @return The {@link Class} object of the immediate caller.
     * @throws SecurityException if a security manager is present and denies access to the stack trace.
     * @since 3.3.1-SNAPSHOT
     */
    public static Class<?> getCallerClass() {
        return getCallerClass(3);
    }

    /**
     * Retrieves the class of the caller at a specified depth in the current thread's stack trace.
     *
     * <p>The depth is controlled by the {@code level} parameter, which specifies how many
     * frames to skip in the stack trace. A {@code level} of 1 corresponds to the immediate caller
     * of this method, a {@code level} of 2 corresponds to the caller's caller, and so on.</p>
     *
     * @param level The number of stack frames to skip to find the desired caller's class.
     *              Must be greater than or equal to 1. Usually needs to be 2.
     * @return The {@link Class} object of the caller at the specified depth.
     * @throws IllegalArgumentException If the provided {@code level} is less than 1.
     * @throws SecurityException        If a security manager is present and denies access to the stack trace.
     * @since 3.3.1-SNAPSHOT
     */
    public static Class<?> getCallerClass(@Range(from = 1, to = Integer.MAX_VALUE) int level) {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.skip(level)
                        .findFirst()
                        .map(StackWalker.StackFrame::getDeclaringClass)
                        .orElseThrow()
                );
    }

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
     * Creates a new instance of a class.
     *
     * @param type The class type of the desired class.
     * @param args The args for the constructor.
     * @param <T>  The desired result type.
     * @return A new instance of the given class type.
     * @throws IllegalStateException if no matching constructor is found for the args.
     * @throws RuntimeException      if the instantiation failed.
     * @since 3.4.0-SNAPSHOT
     */
    public static <T> @NotNull T getNewInstance(@NotNull Class<T> type, @NotNull Object... args) {
        Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);

        if (!isConstructorPresent(type, argTypes))
            throw new IllegalStateException("No constructor found for " + type.getSimpleName() + "(" +
                    String.join(", ", Arrays.stream(argTypes).map(Class::getSimpleName).toList()) + ")!");

        Constructor<T> constructor = getConstructor(type, argTypes);
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not a new instance of " + type.getSimpleName() + "!", e);
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
     * @param base  The base class at which will be stopped, when no generics are found. (exklusive)
     * @param index The index of the generic.
     * @param <T>   The type of handler.
     * @return The class type corresponding to the handler's generic type.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> extractGeneric(Class<?> clazz, Class<?> base, @Range(from = 0, to = Integer.MAX_VALUE) int index) {
        try {
            Type superclass = clazz.getGenericSuperclass();
            if (superclass instanceof ParameterizedType type)
                // Subtract the length by one to make Integer.MAX_VALUE possible as index
                if ((type.getActualTypeArguments().length - 1) >= index) {
                    Type t = type.getActualTypeArguments()[index];

                    if (t instanceof ParameterizedType sub)
                        return (Class<T>) sub.getRawType();

                    return (Class<T>) t;
                }
        } catch (ClassCastException ignored) {
        }

        if (!Object.class.equals(clazz.getSuperclass()) && base.isAssignableFrom(clazz.getSuperclass()))
            return extractGeneric(clazz.getSuperclass(), base, index);

        return null;
    }

    /**
     * Extracts the generic type {@link T} from a given class.
     *
     * @param clazz The class loader from which to extract the generic type.
     * @param index The index of the generic.
     * @param <T>   The type of service handled by the service loader.
     * @return The {@link Class} object representing the generic type {@link T}, or null if unable to extract.
     * @see ParameterizedType
     * @see Class#getGenericInterfaces()
     * @see ParameterizedType#getActualTypeArguments()
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> Class<T> extractGenericInterface(Class<?> clazz, @Range(from = 0, to = Integer.MAX_VALUE) int index) {
        return Arrays.stream(clazz.getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType)
                .map(type -> (ParameterizedType) type)
                .filter(type -> (type.getActualTypeArguments().length - 1) >= index)
                .map(type -> (Class<T>) type.getActualTypeArguments()[index])
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a given annotation is present on the object.
     *
     * @param obj        The object in which to search for the annotation.
     * @param annotation The class of the annotation to search for.
     * @param <A>        The type of the annotation.
     * @return {@code true} if the annotation is present, {@code false} otherwise.
     * @since 3.4.0-SNAPSHOT
     */
    public static <A extends Annotation> boolean isAnnotationPresent(Object obj, Class<A> annotation) {
        if (obj instanceof Method method) return method.isAnnotationPresent(annotation);
        if (obj instanceof Class<?> clazz) return clazz.isAnnotationPresent(annotation);
        return obj.getClass().isAnnotationPresent(annotation);
    }

    /**
     * This method searches for an annotation of the specified type in an object.
     *
     * @param element    The element in which to search for the annotation.
     * @param annotation The class of the annotation to search for.
     * @param <A>        The type of the annotation.
     * @return The found annotation or null if none is found.
     */
    public static <A extends Annotation> A retrieveRawAnnotation(AnnotatedElement element, Class<A> annotation) {
        return element.getDeclaredAnnotation(annotation);
    }

    /**
     * Retrieves the value of a specified annotation attribute from an object.
     *
     * @param <A>             The type of the annotation.
     * @param <T>             The type of the attribute value.
     * @param element         The element containing the annotation.
     * @param annotationClass The class of the annotation.
     * @param type            The class of the targeted type.
     * @param fallback        Defines if the default value should be returned.
     * @return The value of the specified annotation attribute.
     * @throws NoSuchMethodException     If the attribute's getter method is not found.
     * @throws InvocationTargetException If there is an issue invoking the getter method.
     * @throws IllegalAccessException    If there is an access issue with the getter method.
     */
    public static <A extends Annotation, T> T retrieveValueOfAnnotation(AnnotatedElement element, Class<A> annotationClass, Class<T> type, boolean fallback) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        A annotation = retrieveRawAnnotation(element, annotationClass);
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
     * Checks if the object can be cast to a targeted type and casts it.
     *
     * @param o    The value which should be cast to the targeted type.
     * @param type The class of the targeted type
     * @param <T>  The targeted type
     * @return Returns the cast value or null if not cast able.
     */
    public static @Nullable <T> T castTo(Object o, Class<T> type) {
        return type.isInstance(o) ? type.cast(o) : null;
    }

}
