package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.logging.Logger;

import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Class responsible for loading addons dynamically into the CraftsNet framework.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.0
 * @see CraftsNetClassLoader
 * @since 3.0.3-SNAPSHOT
 */
public final class AddonClassLoader extends CraftsNetClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private static final Set<AddonClassLoader> addonLoaders = new CopyOnWriteArraySet<>();

    private final Logger logger;

    private final Set<AddonConfiguration> ignoreNotDepended = new HashSet<>();

    private final String addonName;
    private final List<String> depends;
    private final AddonConfiguration addon;

    /**
     * Constructs an AddonClassLoader with the specified addon manager, addon configuration, and URLs.
     *
     * @param craftsNet     The CraftsNet instance which instantiates this classloader
     * @param configuration The configuration of the addon.
     */
    AddonClassLoader(CraftsNet craftsNet, AddonConfiguration configuration) {
        super(craftsNet, configuration.classpath(), ClassLoader.getSystemClassLoader());
        this.logger = this.getCraftsNet().logger();

        addonLoaders.add(this);
        this.addon = configuration;

        Json json = addon.json();
        this.addonName = json.getString("name");
        this.depends = new ArrayList<>();
        if (json.contains("depends")) this.depends.addAll(json.getStringList("depends"));
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

        if (lookup) {
            for (AddonClassLoader loader : addonLoaders)
                try {
                    Class<?> result = loader.loadClass0(name, resolve, false);

                    if (result.getClassLoader() instanceof AddonClassLoader usedClassLoader) {
                        AddonConfiguration usedAddonConfig = usedClassLoader.addon;
                        String usedAddonName = usedAddonConfig.json().getString("name");

                        if (usedAddonConfig != addon && !ignoreNotDepended.contains(addon) && !depends.contains(usedAddonName)) {
                            logger.warning(addonName + " loaded " + name + " from " + usedAddonName + " which is not marked as dependent!");
                            ignoreNotDepended.add(addon);
                        }
                    }

                    return result;
                } catch (ClassNotFoundException ignored) {
                }

            var dependencyLoaders = addon.dependencyLoaders();
            if (dependencyLoaders != null && dependencyLoaders.length >= 1)
                try {
                    return dependencyLoaders[0].loadClass(name, resolve);
                } catch (ClassNotFoundException ignored) {
                }
        }

        throw new ClassNotFoundException(name);
    }

    /**
     * Finds the class with the specified binary name.
     *
     * @param name The binary name of the class to be found.
     * @return The {@code Class} object representing the class, or {@code null} if the class could not be found.
     * @throws ClassNotFoundException If the class could not be found.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("de.craftsblock.craftsnet.")) {
            throw new ClassNotFoundException(name);
        }
        return super.findClass(name);
    }

    /**
     * Get a list of all {@link AddonClassLoader} which are currently active.
     *
     * @return The set of {@link AddonClassLoader}.
     */
    public static Set<AddonClassLoader> getAddonLoaders() {
        return Collections.unmodifiableSet(addonLoaders);
    }

}
