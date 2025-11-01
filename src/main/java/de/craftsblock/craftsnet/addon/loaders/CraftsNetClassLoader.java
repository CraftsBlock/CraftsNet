package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A sealed marker interface for class loaders that are associated with a
 * {@link CraftsNet} instance.
 * <p>
 * Implementations of this interface represent class loader implementations that
 * maintain a link to a {@link CraftsNet} object (for example, to access addon
 * or runtime-specific states). The interface also provides convenient static
 * helper methods to resolve the {@link CraftsNet} instance from the class loader
 * of the calling class or any arbitrary {@link Class} object, and to check whether
 * a given class was loaded by a {@link CraftsNetClassLoader}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 2.0.0
 * @see CraftsNet
 * @see CraftsNetUrlClassLoader
 * @since 3.5.0
 */
public sealed interface CraftsNetClassLoader permits CraftsNetUrlClassLoader {

    /**
     * Retrieves the {@link CraftsNet} instance which this class loader is linked
     * to.
     *
     * @return The {@link CraftsNet} instance.
     */
    @NotNull CraftsNet getCraftsNet();

    /**
     * Attempts to retrieve the {@link CraftsNet} instance associated with the class loader
     * of the calling class. Returns {@code null} if the class was not loaded by a
     * {@link CraftsNetUrlClassLoader}.
     *
     * @return The associated {@link CraftsNet} instance, or {@code null} if unavailable.
     */
    static @Nullable CraftsNet retrieveCraftsNet() {
        Class<?> caller = ReflectionUtils.getCallerClass();
        return retrieveCraftsNet(caller);
    }

    /**
     * Attempts to retrieve the {@link CraftsNet} instance associated with the class loader
     * of the given class. Returns {@code null} if the class was not loaded by a
     * {@link CraftsNetClassLoader}.
     *
     * @param type The class whose class loader should be checked.
     * @return The associated {@link CraftsNet} instance, or {@code null} if unavailable.
     */
    static @Nullable CraftsNet retrieveCraftsNet(Class<?> type) {
        if (!isCraftsNetLoaded(type)) return null;

        ClassLoader classLoader = type.getClassLoader();
        return ((CraftsNetClassLoader) classLoader).getCraftsNet();
    }

    /**
     * Checks whether the calling class was loaded by a {@link CraftsNetClassLoader}.
     *
     * @return {@code true} if the calling class was loaded by a {@link CraftsNetClassLoader}, {@code false} otherwise.
     */
    static boolean isCraftsNetLoaded() {
        Class<?> caller = ReflectionUtils.getCallerClass();
        return isCraftsNetLoaded(caller);
    }

    /**
     * Checks whether the given class was loaded by a {@link CraftsNetClassLoader}.
     *
     * @param type The class to check.
     * @return {@code true} if the class was loaded by a {@link CraftsNetClassLoader}, {@code false} otherwise.
     */
    static boolean isCraftsNetLoaded(Class<?> type) {
        ClassLoader classLoader = type.getClassLoader();
        return classLoader instanceof CraftsNetClassLoader;
    }

}
