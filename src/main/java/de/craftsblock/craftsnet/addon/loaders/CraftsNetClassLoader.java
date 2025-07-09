package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A specialized class loader for loading classes and resources in the context of a {@link CraftsNet} instance.
 * This class loader supports parallel class loading and associates each loaded class with a specific
 * {@link CraftsNet} context.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see URLClassLoader
 * @since 3.5.0
 */
public abstract class CraftsNetClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final @NotNull CraftsNet craftsNet;

    /**
     * Constructs a new {@link CraftsNetClassLoader} with the specified {@link CraftsNet} instance and URLs.
     * Uses the system class loader as the parent.
     *
     * @param craftsNet The {@link CraftsNet} instance this class loader is associated with.
     * @param urls      The list of URLs from which to load classes and resources.
     */
    public CraftsNetClassLoader(@NotNull CraftsNet craftsNet, @NotNull URL @NotNull [] urls) {
        this(craftsNet, urls, ClassLoader.getSystemClassLoader());
    }

    /**
     * Constructs a new {@link CraftsNetClassLoader} with the specified {@link CraftsNet} instance,
     * URLs, and parent class loader.
     *
     * @param craftsNet The {@link CraftsNet} instance this class loader is associated with.
     * @param urls      The list of URLs from which to load classes and resources.
     * @param parent    The parent class loader for delegation.
     */
    public CraftsNetClassLoader(@NotNull CraftsNet craftsNet, @NotNull URL @NotNull [] urls, @NotNull ClassLoader parent) {
        super(urls, parent);

        this.craftsNet = craftsNet;
    }

    /**
     * Retrieves the {@link CraftsNet} instance which this class loader is linked
     * to.
     *
     * @return The {@link CraftsNet} instance.
     */
    public @NotNull CraftsNet getCraftsNet() {
        return craftsNet;
    }

    /**
     * Attempts to retrieve the {@link CraftsNet} instance associated with the class loader
     * of the calling class. Returns {@code null} if the class was not loaded by a
     * {@link CraftsNetClassLoader}.
     *
     * @return The associated {@link CraftsNet} instance, or {@code null} if unavailable.
     */
    public static @Nullable CraftsNet retrieveCraftsNet() {
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
    public static @Nullable CraftsNet retrieveCraftsNet(Class<?> type) {
        if (!isCraftsNetLoaded(type)) return null;

        ClassLoader classLoader = type.getClassLoader();
        return ((CraftsNetClassLoader) classLoader).getCraftsNet();
    }

    /**
     * Checks whether the calling class was loaded by a {@link CraftsNetClassLoader}.
     *
     * @return {@code true} if the calling class was loaded by a {@link CraftsNetClassLoader}, {@code false} otherwise.
     */
    public static boolean isCraftsNetLoaded() {
        Class<?> caller = ReflectionUtils.getCallerClass();
        return isCraftsNetLoaded(caller);
    }

    /**
     * Checks whether the given class was loaded by a {@link CraftsNetClassLoader}.
     *
     * @param type The class to check.
     * @return {@code true} if the class was loaded by a {@link CraftsNetClassLoader}, {@code false} otherwise.
     */
    public static boolean isCraftsNetLoaded(Class<?> type) {
        ClassLoader classLoader = type.getClassLoader();
        return classLoader instanceof CraftsNetClassLoader;
    }

}
