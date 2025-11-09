package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.events.addons.AllAddonsDisabledEvent;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The AddonManager class is responsible for managing addons in the application.
 * It loads and unloads addons, and keeps track of registered addons in a ConcurrentHashMap.
 * Addons can be registered and unregistered through this class.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.3.3
 * @see Addon
 * @see AddonLoader
 * @since 1.0.0-SNAPSHOT
 */
public final class AddonManager {

    private final List<AddonConfiguration> configurations = new ArrayList<>();
    private final long start = System.currentTimeMillis();

    private final CraftsNet craftsNet;
    private final Logger logger;
    private final ConcurrentHashMap<String, Addon> addons = new ConcurrentHashMap<>();

    private final AddonLoader addonLoader;

    /**
     * Constructor for the AddonManager class. It loads and initializes the addons present in the "./addons/" folder.
     * It also adds a shutdown hook to handle cleanup when the application is shut down.
     *
     * @param craftsNet The CraftsNet instance which instantiates this addon manager.
     */
    public AddonManager(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.getLogger();

        this.addonLoader = new AddonLoader(craftsNet);
    }

    /**
     * Adds {@link AddonConfiguration} directly to the configurations which
     * will be loaded on startup.
     *
     * @param configurations The {@link AddonConfiguration} to add.
     * @since 3.4.3
     */
    public void addDirectly(Collection<AddonConfiguration> configurations) {
        synchronized (this.configurations) {
            this.configurations.addAll(configurations);
        }
    }

    /**
     * Performs the startup of the addon system and loads all present
     * addon configurations.
     *
     * @since 3.4.3
     */
    public void startup() {
        synchronized (configurations) {
            if (configurations.isEmpty()) {
                logger.info("No addons found to load");
                return;
            }

            synchronized (addonLoader) {
                logger.info("Load all available addons");
                addonLoader.load(configurations);
                logger.info("Loaded %s addons within %sms", configurations.size(), System.currentTimeMillis() - start);
            }

            configurations.clear();
        }
    }

    /**
     * Load all addons from the addons folder.
     *
     * @throws IOException if there is an I/O error while accessing the addons folder.
     */
    public void fromFiles() throws IOException {
        synchronized (addonLoader) {
            addonLoader.reset();

            Path folder = Path.of("addons");
            logger.debug("Addon folder set to %s", folder.toAbsolutePath().toFile().getAbsolutePath());
            if (Files.notExists(folder) || !Files.isDirectory(folder)) {
                Files.deleteIfExists(folder);
                Files.createDirectories(folder);
            }

            try (Stream<Path> stream = Files.walk(folder, 1)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".jar"))
                        .forEach(addonLoader::update);
            }

            this.addDirectly(addonLoader.load());
        }
    }

    /**
     * Method to stop the AddonManager. It is called during application shutdown.
     */
    public void stop() {
        addons.values().forEach(addon -> {
            logger.info("Disabling addon %s", addon.getName());
            this.unregister(addon);
        });

        craftsNet.getListenerRegistry().call(new AllAddonsDisabledEvent());
        addons.clear();
    }

    /**
     * Registers an addon in the AddonManager.
     *
     * @param addon The addon to be registered.
     */
    public void register(@NotNull Addon addon) {
        addons.put(addon.getName(), addon);
    }

    /**
     * Unregisters an addon from the AddonManager.
     *
     * @param addon The addon to be unregistered.
     */
    public void unregister(@NotNull Addon addon) {
        addons.remove(addon.getName());
        addon.onDisable();
    }

    /**
     * Returns a read-only view of the registered addons in the AddonManager.
     *
     * @return A read-only ConcurrentHashMap containing the registered addons.
     */
    public @Unmodifiable @NotNull Map<String, Addon> getAddons() {
        return Collections.unmodifiableMap(addons);
    }

    /**
     * Retrieves an addon of the specified type from the loaded addons.
     *
     * @param <T>   The type of the addon to retrieve, extending the Addon class.
     * @param addon The class object representing the type of the addon to be retrieved.
     * @return An instance of the specified addon type if found, or {@code null} if not present.
     */
    public <T extends Addon> @Nullable T getAddon(@NotNull Class<T> addon) {
        if (HollowAddon.class.isAssignableFrom(addon))
            throw new IllegalArgumentException(addon.getSimpleName() + "s cannot be retrieved by class, use the name instead!");

        return addons.values().stream()
                .filter(addon::isInstance)
                .map(addon::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves an addon by its name from the loaded addons.
     *
     * @param <T>  The type of the addon to retrieve, extending the Addon class.
     * @param name The name of the addon to be retrieved.
     * @return An instance of the specified addon type if found, or {@code null} if not present.
     * @since 3.3.5-SNAPSHOT
     */
    @SuppressWarnings("unchecked")
    public <T extends Addon> @Nullable T getAddon(String name) {
        return (T) addons.values().stream()
                .filter(addon -> addon.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks whether an {@link Addon} class is registered or not.
     *
     * @param addon The class of the {@link Addon} to check.
     * @return {@code true} if the addon is registered, {@code false} otherwise.
     * @since 3.3.5-SNAPSHOT
     */
    public boolean isRegistered(@NotNull Class<? extends Addon> addon) {
        if (HollowAddon.class.isAssignableFrom(addon)) return false;
        return addons.values().stream().anyMatch(addon::isInstance);
    }

    /**
     * Checks whether an {@link Addon} is registered using its name.
     *
     * @param name The name of the {@link Addon}
     * @return {@code true} if the addon is registered, {@code false} otherwise.
     * @since 3.3.5-SNAPSHOT
     */
    public boolean isRegistered(@NotNull String name) {
        return addons.values().stream().anyMatch(addon -> addon.getName().equalsIgnoreCase(name));
    }

    /**
     * Gets the underlying {@link AddonLoader} which is used for
     * loading the addons.
     *
     * @return The {@link AddonLoader}
     * @since 3.4.3
     */
    @ApiStatus.Internal
    public AddonLoader getAddonLoader() {
        return addonLoader;
    }

}
