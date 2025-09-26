package de.craftsblock.craftsnet.utils.reflection;

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
 * @version 1.4.4
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
     * Ensures that the caller's caller is of one of the specified allowed types.
     * <p>
     * This method is intended to restrict access to internal APIs or methods,
     * by validating that the method is only invoked (indirectly) by allowed classes.
     * If the caller's caller is not assignable to any of the specified classes, an
     * {@link IllegalStateException} is thrown.
     *
     * @param allowed The list of classes that are allowed to indirectly call the current method.
     * @throws IllegalStateException If the caller's caller is not permitted to call the method.
     * @since 3.5.0
     */
    public static void restrictToCallers(Class<?>... allowed) {
        Class<?> caller = ReflectionUtils.getCallerClass();
        Class<?> callersCaller = ReflectionUtils.getCallerClass(4);

        for (Class<?> allow : allowed)
            if (allow.isAssignableFrom(callersCaller)) return;

        throw new IllegalStateException(callersCaller.getName() + " is not permitted to call a " + caller.getSimpleName());
    }

    /**
     * Checks if a constructor is present in the specified class with the provided argument types.
     *
     * @param clazz The class to check for the constructor.
     * @param args  The argument types to check for.
     * @return true if the constructor is present, false otherwise.
     */
    public static boolean isConstructorPresent(Class<?> clazz, Class<?>... args) {
        return findConstructor(clazz, args) != null;
    }

    /**
     * Gets the constructor of the specified class with the provided argument types.
     *
     * @param clazz The class to get the constructor for.
     * @param args  The argument types for the constructor.
     * @param <T>   The type to load the constructor from.
     * @return The constructor of the class.
     * @throws RuntimeException If no constructor is found.
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... args) {
        if (clazz == null) return null;

        for (Constructor<?> constructor : clazz.getDeclaredConstructors())
            if (areArgsCompatible(constructor, args)) return (Constructor<T>) constructor;

        return null;
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

        Constructor<T> constructor = findConstructor(type, argTypes);
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not create a new instance of " + type.getSimpleName() + "!", e);
        }
    }

    /**
     * Sets a field in the given object using reflection.
     *
     * @param name   The name of the field to set.
     * @param target The object whose field needs to be set.
     * @param value  The value to set the field to.
     */
    public static void setField(String name, Object target, Object value) {
        Field field = findField(target.getClass(), name);
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Can not set field %s of class %s!".formatted(
                    name, target.getClass().getSimpleName()
            ), e);
        }
    }

    /**
     * Finds a field with the given name in the specified class, including inherited classes.
     *
     * @param clazz The class to search for the field.
     * @param name  The name of the field to find.
     * @return The field if found, otherwise null.
     */
    public static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name); // Attempt to get the specified field from the current class
            } catch (NoSuchFieldException ignored) {
            }

            clazz = clazz.getSuperclass(); // Move to the superclass for further field search
        }

        return null;
    }

    /**
     * Invokes a method with the given name and arguments on the specified owner object.
     *
     * @param owner The object on which the method is to be invoked.
     * @param name  The name of the method.
     * @param args  The arguments to pass to the method.
     * @return The result returned by the invoked method.
     * @throws IllegalStateException If no matching method is found.
     * @throws RuntimeException      If the method invocation fails.
     * @since 3.5.0
     */
    public static Object invokeMethod(Object owner, String name, Object... args) {
        Class<?> type = owner.getClass();
        Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);

        Method method = findMethod(type, name, argTypes);
        if (method == null)
            throw new IllegalStateException("No method %s(%s) found in %s!".formatted(
                    name, String.join(", ", Arrays.stream(argTypes).map(Class::getSimpleName).toList()),
                    type.getSimpleName()
            ));

        return invokeMethod(owner, method, args);
    }

    /**
     * Invokes a specific method with the given arguments on the specified owner object.
     *
     * @param owner  The object on which the method is to be invoked.
     * @param method The method to invoke.
     * @param args   The arguments to pass to the method.
     * @return The result returned by the invoked method.
     * @since 3.5.1
     */
    public static Object invokeMethod(Object owner, Method method, Object... args) {
        try {
            boolean isStatic = Modifier.isStatic(method.getModifiers());

            method.setAccessible(true);
            return method.invoke(isStatic ? null : owner, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Could not invoke " + method.toGenericString(), e);
        }
    }

    /**
     * Finds a method by name and argument types in the given class, its superclasses, and interfaces.
     * <p>
     * <b>Important:</b> Bridge and synthetic methods generated by the compiler are ignored.
     * Only developer-defined methods are considered.
     *
     * @param type The class to search for the method.
     * @param name The name of the method.
     * @param args The argument types of the method.
     * @return The {@link Method} if found; {@code null} otherwise.
     * @since 3.5.0
     */
    public static Method findMethod(Class<?> type, String name, Class<?>... args) {
        if (type == null) return null;

        for (Method method : type.getDeclaredMethods()) {
            if (method.isBridge() || method.isSynthetic()) continue;
            if (!method.getName().equals(name)) continue;
            if (!areArgsCompatible(method, args)) continue;

            return method;
        }

        if (Object.class.equals(type)) return null;

        // Search the superclass
        var fromSuperclass = findMethod(type.getSuperclass(), name, args);
        if (fromSuperclass != null) return fromSuperclass;

        // Search the interfaces
        for (Class<?> iface : type.getInterfaces()) {
            var method = findMethod(iface, name, args);
            if (method == null) continue;
            return method;
        }

        return null;
    }

    /**
     * Checks whether the provided argument types are compatible with the parameter types of the executable.
     *
     * <p>Supports checking compatibility for varargs methods.</p>
     *
     * @param executable The {@link Executable} (method or constructor) to check against.
     * @param args       The argument types to verify.
     * @return {@code true} if the argument types are compatible; {@code false} otherwise.
     * @since 3.5.0
     */
    public static boolean areArgsCompatible(Executable executable, Class<?>... args) {
        Class<?>[] paramTypes = executable.getParameterTypes();
        boolean isVarArgs = executable.isVarArgs();
        int fixedParamCount = paramTypes.length - (isVarArgs ? 1 : 0);

        if (args.length < fixedParamCount || (!isVarArgs && args.length != paramTypes.length))
            return false;

        for (int i = 0; i < fixedParamCount; i++)
            if (!TypeUtils.isAssignable(paramTypes[i], args[i])) return false;

        if (!isVarArgs || args.length == paramTypes.length - 1) return true;

        // If the last arg is an array check the vararg as array
        int lastIndexOfArg = args.length - 1;
        if (args.length == paramTypes.length && args[lastIndexOfArg].isArray())
            return TypeUtils.isAssignable(paramTypes[lastIndexOfArg], args[lastIndexOfArg]);

        // Check all remaining args if it matches with the vararg
        Class<?> varArgType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedParamCount; i < args.length; i++)
            if (!TypeUtils.isAssignable(varArgType, args[i])) return false;

        return true;
    }

    /**
     * Extracts the generic type parameter from a Class.
     *
     * @param clazz The Class to extract the generic type from.
     * @param index The index of the generic.
     * @param <T>   The type of handler.
     * @return The class type corresponding to the handler's generic type.
     * @since 3.4.3-SNAPSHOT
     */
    public static <T> Class<T> extractGeneric(Class<?> clazz, @Range(from = 0, to = Integer.MAX_VALUE) int index) {
        return extractGeneric(clazz, Object.class, index);
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
                    return (Class<T>) TypeUtils.convertTypeToClass(t);
                }
        } catch (ClassCastException ignored) {
        }

        if (!Object.class.equals(clazz.getSuperclass()) && TypeUtils.isAssignable(base, clazz.getSuperclass()))
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
        Type type = Arrays.stream(clazz.getGenericInterfaces())
                .filter(t -> t instanceof ParameterizedType)
                .map(t -> (ParameterizedType) t)
                .filter(t -> (t.getActualTypeArguments().length - 1) >= index)
                .map(t -> t.getActualTypeArguments()[index])
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not extract generic interface from class %s at index %s!".formatted(
                        clazz.getName(), index
                )));

        return (Class<T>) TypeUtils.convertTypeToClass(type);
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
        return type != null && type.isInstance(o) ? type.cast(o) : null;
    }

}
