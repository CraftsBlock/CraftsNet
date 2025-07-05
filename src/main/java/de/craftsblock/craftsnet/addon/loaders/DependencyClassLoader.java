package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftsnet.CraftsNet;

import java.net.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class responsible for loading dependencies dynamically into the CraftsNet framework.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see CraftsNetClassLoader
 * @since 3.4.3
 */
public final class DependencyClassLoader extends CraftsNetClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private static final Map<URI, DependencyClassLoader> dependenciesLoaders = new ConcurrentHashMap<>();

    private final URL url;

    /**
     * Constructs an AddonClassLoader with the specified addon manager, addon configuration, and URLs.
     *
     * @param craftsNet The CraftsNet instance which instantiates this classloader
     * @param url       The url which contains the source of the dependency.
     */
    DependencyClassLoader(CraftsNet craftsNet, URL url) {
        super(craftsNet, new URL[]{url}, ClassLoader.getSystemClassLoader());

        this.url = url;
    }

    /**
     * Loads the class with the specified name, optionally linking it after loading.
     *
     * @param name    The binary name of the class to be loaded.
     * @param resolve {@code true} to resolve the class; {@code false} to skip resolution.
     * @return The {@code Class} object representing the loaded class.
     * @throws ClassNotFoundException If the class could not be found.
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true);
    }

    /**
     * Loads the class with the specified name, optionally linking it after loading.
     *
     * @param name    The binary name of the class to be loaded.
     * @param resolve {@code true} to resolve the class; {@code false} to skip resolution.
     * @param lookup  {@code true} to perform a lookup if the class is not found in this loader;
     *                {@code false} to skip lookup.
     * @return The {@code Class} object representing the loaded class.
     * @throws ClassNotFoundException If the class could not be found.
     */
    private Class<?> loadClass0(String name, boolean resolve, boolean lookup) throws ClassNotFoundException {
        try {
            Class<?> result = super.loadClass(name, resolve);
            if (lookup || result.getClassLoader() == this) return result;
        } catch (ClassNotFoundException ignored) {
        }

        if (lookup)
            for (DependencyClassLoader loader : dependenciesLoaders.values())
                try {
                    return loader.loadClass0(name, resolve, false);
                } catch (ClassNotFoundException ignored) {
                }

        throw new ClassNotFoundException(name);
    }

    /**
     * Retrieves the {@link URL} which this {@link DependencyClassLoader} loads classes
     * from.
     *
     * @return The source {@link URL}.
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Get a list of all {@link DependencyClassLoader} which are currently active.
     *
     * @return The set of {@link DependencyClassLoader}.
     */
    public static Set<DependencyClassLoader> getDependenciesLoaders() {
        return Set.of(dependenciesLoaders.values().toArray(DependencyClassLoader[]::new));
    }

    /**
     * Creates a new {@link DependencyClassLoader} for a specific {@link URL}. If a
     * {@link DependencyClassLoader} matching the {@link URL} already exists, no new
     * {@link DependencyClassLoader} is created and the existing one is returned.
     *
     * @param craftsNet The instance of {@link CraftsNet} which creates the {@link DependencyClassLoader}.
     * @param url       The {@link URL} containing the content of the dependency.
     * @return An {@link DependencyClassLoader} wrapping the specific {@link URL}.
     */
    public static DependencyClassLoader safelyNew(CraftsNet craftsNet, URL url) {
        try {
            return dependenciesLoaders.computeIfAbsent(url.toURI(), u -> new DependencyClassLoader(craftsNet, url));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not convert the url %s to an uri!".formatted(url), e);
        }
    }

}
