package de.craftsblock.craftsnet.autoregister.meta;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.utils.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The {@link AutoRegisterInfo} holds information about a class to be processed in an auto registration
 * process. It contains the class name, the annotation associated with the class, the class loader,
 * and a list of parent types (superclasses and interfaces) of the class.
 * Optional it contains the addon that the source is located in. If no addons was set, the source is
 * not located inside an addon.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 2.0.1
 * @since 3.2.0-SNAPSHOT
 */
public class AutoRegisterInfo {

    private final @NotNull String className;
    private final @Nullable Addon bounding;
    private final @NotNull Annotation annotation;
    private final @NotNull ClassLoader loader;
    @NotNull
    private final @Unmodifiable List<String> parentTypes;

    private Object instantiated;

    /**
     * Constructs an {@code AutoRegisterInfo} object with the given parameters.
     *
     * @param className   The fully qualified name of the class to be registered.
     * @param bounding    The optional {@link Addon} that contains the class. Can be {@code null} if not inside an addon.
     * @param annotation  The annotation associated with the class.
     * @param loader      The {@link ClassLoader} that was used to load the class.
     * @param parentTypes A list of parent types (superclasses and interfaces) of the class.
     */
    public AutoRegisterInfo(@NotNull String className, @Nullable Addon bounding, @NotNull Annotation annotation,
                            @NotNull ClassLoader loader, @NotNull List<String> parentTypes) {
        this.className = className;
        this.bounding = bounding;
        this.annotation = annotation;
        this.loader = loader;
        this.parentTypes = Collections.unmodifiableList(parentTypes);
    }

    /**
     * Returns an instance of the target class. If the instance type requires a new object
     * each time, a fresh instance is created. Otherwise, a cached instance is returned.
     *
     * @param craftsNet The {@link CraftsNet} instance used for dependency injection.
     * @return An instance of the target class.
     * @since 3.3.2-SNAPSHOT
     */
    public Object getInstantiated(CraftsNet craftsNet) {
        if (!(annotation instanceof AutoRegister autoRegister) || autoRegister.instantiate().equals(Instantiate.NEW_INSTANCE))
            return getNewInstance(craftsNet);

        if (instantiated != null) return instantiated;
        return instantiated = getNewInstance(craftsNet);
    }

    /**
     * Creates a new instance of the target class by dynamically selecting an appropriate constructor.
     * The method tries the following constructors (in order):
     * <ol>
     *     <li>A no-argument constructor</li>
     *     <li>A constructor accepting an {@link Addon} (if available)</li>
     *     <li>A constructor accepting the bounding {@link Addon} (if available)</li>
     *     <li>A constructor accepting a {@link CraftsNet} instance</li>
     * </ol>
     *
     * <p>If no suitable constructor is found, an exception is thrown.</p>
     *
     * @param craftsNet The {@link CraftsNet} instance used for dependency injection.
     * @return A new instance of the target class.
     * @throws RuntimeException if the class cannot be instantiated.
     * @since 3.3.2-SNAPSHOT
     */
    private Object getNewInstance(CraftsNet craftsNet) {
        Class<?> clazz;
        try {
            clazz = getLoader().loadClass(getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Constructor<?> constructor;
        Object[] constructorArgs;
        if (ReflectionUtils.isConstructorPresent(clazz)) {
            constructor = ReflectionUtils.getConstructor(clazz);
            constructorArgs = new Object[0];
        } else if (hasBounding() && ReflectionUtils.isConstructorPresent(clazz, Addon.class)) {
            constructor = ReflectionUtils.getConstructor(clazz, Addon.class);
            constructorArgs = new Object[]{getBounding()};
        } else if (hasBounding() && ReflectionUtils.isConstructorPresent(clazz, getBounding().getClass())) {
            constructor = ReflectionUtils.getConstructor(clazz, getBounding().getClass());
            constructorArgs = new Object[]{getBounding()};
        } else {
            constructor = ReflectionUtils.getConstructor(clazz, CraftsNet.class);
            constructorArgs = new Object[]{craftsNet};
        }

        try {
            return constructor.newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the fully qualified class name of the target class.
     *
     * @return The class name as a {@link String}.
     */
    public @NotNull String getClassName() {
        return className;
    }

    /**
     * Returns the {@link Addon} associated with this class, if any.
     *
     * @return The {@link Addon} instance or {@code null} if no addon is associated.
     */
    public @Nullable Addon getBounding() {
        return bounding;
    }

    /**
     * Checks whether this class is associated with an {@link Addon}.
     *
     * @return {@code true} if an addon is associated, otherwise {@code false}.
     * @since 3.3.2-SNAPSHOT
     */
    public boolean hasBounding() {
        return bounding != null;
    }

    /**
     * Returns the annotation associated with this class.
     *
     * @return The associated {@link Annotation}.
     */
    public @NotNull Annotation getAnnotation() {
        return annotation;
    }

    /**
     * Returns the class loader used to load this class.
     *
     * @return The {@link ClassLoader} instance.
     */
    public @NotNull ClassLoader getLoader() {
        return loader;
    }

    /**
     * Returns a list of parent types (superclasses and interfaces) of the class.
     *
     * @return An unmodifiable list of parent type names.
     */
    public @NotNull @Unmodifiable List<String> getParentTypes() {
        return parentTypes;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares two {@code AutoRegisterInfo} objects for equality based on their class name and annotation.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoRegisterInfo that = (AutoRegisterInfo) o;
        return Objects.equals(className, that.className) && Objects.equals(annotation, that.annotation);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calculates the hash code based on the class name and annotation.</p>
     */
    @Override
    public int hashCode() {
        return Objects.hash(className, annotation);
    }

    /**
     * Creates a new instance of {@link AutoRegisterInfo}.
     *
     * @param className   The name of the class.
     * @param bounding    The addon that the auto register info is from. Nullable if the register info does not come from an addon.
     * @param annotation  The annotation associated with the class.
     * @param loader      The class loader that was used to load the class.
     * @param parentTypes A list of the names of the parent types (superclasses and interfaces) of the class.
     * @return A new instance of {@link AutoRegisterInfo}.
     */
    public static AutoRegisterInfo of(@NotNull String className, @Nullable Addon bounding, @NotNull Annotation annotation,
                                      @NotNull ClassLoader loader, @NotNull List<String> parentTypes) {
        return new AutoRegisterInfo(className, bounding, annotation, loader, parentTypes);
    }

}
