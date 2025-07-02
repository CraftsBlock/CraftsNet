package de.craftsblock.craftsnet.autoregister.meta;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.autoregister.meta.constructors.ConstructorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * The {@link AutoRegisterInfo} holds information about a class to be processed in an auto registration
 * process. It contains the class name, the annotation associated with the class, the class loader,
 * and a list of parent types (superclasses and interfaces) of the class.
 * Optional it contains the addon that the source is located in. If no addons was set, the source is
 * not located inside an addon.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 2.1.2
 * @since 3.2.0-SNAPSHOT
 */
public class AutoRegisterInfo {

    private final @NotNull String className;
    private final @Nullable Collection<Addon> bounding;
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
    public AutoRegisterInfo(@NotNull String className, @Nullable Collection<Addon> bounding, @NotNull Annotation annotation,
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
        if (!(annotation instanceof AutoRegister autoRegister) || autoRegister.instantiate().equals(Instantiate.NEW))
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

        EnumMap<ConstructorType, Collection<Constructor<?>>> constructors = new EnumMap<>(ConstructorType.class);
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != 0 && Arrays.stream(constructor.getParameterTypes())
                    .noneMatch(param -> CraftsNet.class.isAssignableFrom(param) || Addon.class.isAssignableFrom(param)))
                continue;

            constructors.computeIfAbsent(ConstructorType.estimateType(constructor), c -> new ArrayList<>()).add(constructor);
        }

        // Remove ignored constructors
        constructors.remove(ConstructorType.IGNORED);

        Constructor<?> constructor = null;
        Object[] constructorArgs = null;

        Collection<Addon> addons = craftsNet.getAddonManager().getAddons().values();
        for (Constructor<?> con : constructors.values().stream().flatMap(Collection::stream).toList()) {
            Collection<Object> args = new ArrayList<>();

            for (Class<?> type : con.getParameterTypes())
                if (CraftsNet.class.isAssignableFrom(type)) args.add(craftsNet);
                else if (Addon.class.isAssignableFrom(type)) {
                    Optional<Addon> matchingAddon = addons.stream()
                            .filter(addon -> addon.getClass().isAssignableFrom(type))
                            .findFirst();

                    if (matchingAddon.isPresent()) args.add(matchingAddon.get());
                    else break;
                } else break;

            if (args.size() != con.getParameterCount()) continue;
            constructor = con;
            constructorArgs = args.toArray();
            break;
        }

        if (constructor == null)
            throw new IllegalStateException("No suitable constructor found for autoregister target " + clazz);

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
    public @Nullable Collection<Addon> getBounding() {
        return bounding;
    }

    /**
     * Checks whether this class is associated with an {@link Addon}.
     *
     * @return {@code true} if an addon is associated, otherwise {@code false}.
     * @since 3.3.2-SNAPSHOT
     */
    public boolean hasBounding() {
        return bounding != null && !bounding.isEmpty();
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
     * @param bounding    The list of addons that the auto register info is from. Nullable if the register info does not come from an addon.
     * @param annotation  The annotation associated with the class.
     * @param loader      The class loader that was used to load the class.
     * @param parentTypes A list of the names of the parent types (superclasses and interfaces) of the class.
     * @return A new instance of {@link AutoRegisterInfo}.
     */
    public static AutoRegisterInfo of(@NotNull String className, @Nullable Collection<Addon> bounding, @NotNull Annotation annotation,
                                      @NotNull ClassLoader loader, @NotNull List<String> parentTypes) {
        return new AutoRegisterInfo(className, bounding, annotation, loader, parentTypes);
    }

}
