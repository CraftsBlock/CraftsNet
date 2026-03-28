package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftsnet.CraftsNet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * An abstract base implementation of a {@link URLClassLoader} that is tied to a specific
 * {@link CraftsNet} instance.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see CraftsNetClassLoader
 * @see AddonClassLoader
 * @see DependencyClassLoader
 * @since 3.5.5
 */
@ApiStatus.Internal
public sealed abstract class CraftsNetUrlClassLoader<T extends CraftsNetUrlClassLoader<?>> extends URLClassLoader implements CraftsNetClassLoader permits AddonClassLoader, DependencyClassLoader {

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
     * <p>
     * If the requested resource is located under {@code META-INF/services},
     * resources from sibling class loaders are included as well.
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     * @since 3.7.0
     */
    @Override
    public final Enumeration<URL> getResources(String name) throws IOException {
        return this.getResources0(name, name.matches("^/?META-INF/services/.*$"));
    }

    /**
     * Internal helper method for resource lookup.
     * <p>
     * Depending on the {@code lookup} flag, this method optionally aggregates
     * resources from sibling class loaders.
     *
     * @param name   The name of the resource to search for
     * @param lookup Whether sibling class loaders should be queried
     * @return An {@link Enumeration} of all matching resources
     * @throws IOException If an I/O error occurs
     */
    final Enumeration<URL> getResources0(String name, boolean lookup) throws IOException {
        Enumeration<URL> enumeration = super.getResources(name);
        if (!lookup) {
            return enumeration;
        }

        List<URL> resources = new ArrayList<>();
        for (T loader : getSiblings()) {
            Enumeration<URL> sub = loader.getResources0(name, false);

            while (sub.hasMoreElements()) {
                resources.add(sub.nextElement());
            }
        }

        return Collections.enumeration(resources);
    }

    /**
     * Returns all sibling class loaders associated with this class loader.
     * <p>
     * Sibling class loaders are typically used to share resources such as
     * service provider configurations.
     *
     * @return A collection of sibling class loaders
     */
    abstract Collection<T> getSiblings();

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public final @NotNull CraftsNet getCraftsNet() {
        return craftsNet;
    }

}
