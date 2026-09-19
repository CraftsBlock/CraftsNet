package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Class responsible for loading addons dynamically into the CraftsNet framework.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @see CraftsNetUrlClassLoader
 * @since 3.0.3-SNAPSHOT
 */
public final class AddonClassLoader extends CraftsNetUrlClassLoader<AddonClassLoader> {

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
    @ApiStatus.Internal
    public AddonClassLoader(CraftsNet craftsNet, AddonConfiguration configuration) {
        super(craftsNet, configuration.classpath(), ClassLoader.getSystemClassLoader());
        ReflectionUtils.restrictToCallers(AddonConfiguration.class, AddonLoader.class);
        this.logger = this.getCraftsNet().getLogger();

        addonLoaders.add(this);
        this.addon = configuration;

        Json json = addon.json();
        this.addonName = json.getString("name");
        this.depends = new ArrayList<>();
        if (json.contains("depends")) this.depends.addAll(json.getStringList("depends"));
    }

    /**
     * {@inheritDoc}
     *
     * @param name    {@inheritDoc}
     * @param resolve {@inheritDoc}
     * @param lookup  {@inheritDoc}
     * @return {@inheritDoc}
     * @throws ClassNotFoundException {@inheritDoc}
     */
    @Override
    Class<?> loadClass0(String name, boolean resolve, boolean lookup) throws ClassNotFoundException {
        Class<?> parentResult = super.loadClass0(name, resolve, lookup);
        if (parentResult != null) {
            return parentResult;
        }

        for (AddonClassLoader loader : addonLoaders) {
            try {
                Class<?> result = loader.loadClass0(name, resolve, false);

                if (result.getClassLoader() instanceof AddonClassLoader usedClassLoader) {
                    AddonConfiguration usedAddonConfig = usedClassLoader.addon;
                    String usedAddonName = usedAddonConfig.json().getString("name");

                    if (usedAddonConfig != addon && !ignoreNotDepended.contains(addon) && !depends.contains(usedAddonName)) {
                        logger.warning("%s loaded %s from %s which is not marked as dependent!", addonName, name, usedAddonName);
                        ignoreNotDepended.add(addon);
                    }
                }

                return result;
            } catch (ClassNotFoundException ignored) {
            }
        }

        var dependencyLoaders = addon.dependencyLoaders();
        if (dependencyLoaders != null && dependencyLoaders.length >= 1) {
            try {
                return dependencyLoaders[0].loadClass(name, resolve);
            } catch (ClassNotFoundException ignored) {
            }
        }

        throw new ClassNotFoundException(name);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @since 3.7.0
     */
    @Override
    Collection<AddonClassLoader> getSiblings() {
        return addonLoaders;
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
