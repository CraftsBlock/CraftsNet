package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftsnet.CraftsNet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * An abstract base implementation of a {@link URLClassLoader} that is tied to a specific
 * {@link CraftsNet} instance.
 *
 * @author CraftsBlock
 * @version 1.0.0
 * @see CraftsNetClassLoader
 * @see AddonClassLoader
 * @see DependencyClassLoader
 * @since 3.5.5
 */
@ApiStatus.Internal
public sealed abstract class CraftsNetUrlClassLoader extends URLClassLoader implements CraftsNetClassLoader permits AddonClassLoader, DependencyClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final CraftsNet craftsNet;

    /**
     * Constructs a new {@link CraftsNetUrlClassLoader} with the specified {@link CraftsNet} instance and URLs.
     * Uses the system class loader as the parent.
     *
     * @param craftsNet The {@link CraftsNet} instance this class loader is associated with.
     * @param urls      The list of URLs from which to load classes and resources.
     */
    public CraftsNetUrlClassLoader(@NotNull CraftsNet craftsNet, @NotNull URL @NotNull [] urls) {
        this(craftsNet, urls, ClassLoader.getSystemClassLoader());
    }

    /**
     * Constructs a new {@link CraftsNetUrlClassLoader} with the specified {@link CraftsNet} instance,
     * URLs, and parent class loader.
     *
     * @param craftsNet The {@link CraftsNet} instance this class loader is associated with.
     * @param urls      The list of URLs from which to load classes and resources.
     * @param parent    The parent class loader for delegation.
     */
    public CraftsNetUrlClassLoader(@NotNull CraftsNet craftsNet, @NotNull URL @NotNull [] urls, @NotNull ClassLoader parent) {
        super(urls, parent);

        this.craftsNet = craftsNet;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull CraftsNet getCraftsNet() {
        return craftsNet;
    }

}
