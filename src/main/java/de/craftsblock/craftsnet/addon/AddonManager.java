package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The AddonManager class is responsible for managing addons in the application.
 * It loads and unloads addons, and keeps track of registered addons in a ConcurrentHashMap.
 * Addons can be registered and unregistered through this class.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see Addon
 * @see AddonLoader
 * @since CraftsNet-1.0.0
 */
public final class AddonManager {

    private final CraftsNet craftsNet;
    private final Logger logger;

    private final ConcurrentHashMap<String, Addon> addons = new ConcurrentHashMap<>();

    /**
     * Constructor for the AddonManager class. It loads and initializes the addons present in the "./addons/" folder.
     * It also adds a shutdown hook to handle cleanup when the application is shut down.
     *
     * @param craftsNet The CraftsNet instance which instantiates this addon manager.
     * @throws IOException if there is an I/O error while accessing the addons folder.
     */
    public AddonManager(CraftsNet craftsNet) throws IOException {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.logger();

        File folder = new File("./addons/");
        logger.debug("Addon folder set to " + folder.getAbsolutePath());
        if (!folder.isDirectory()) {
            folder.delete();
            folder.mkdirs();
        }

        AddonLoader addonLoader = new AddonLoader(craftsNet);
        for (File file : Objects.requireNonNull(folder.listFiles()))
            if (file.getName().endsWith(".jar")) addonLoader.add(file);
        addonLoader.load(this);
    }

    /**
     * Method to stop the AddonManager. It is called during application shutdown.
     */
    public void stop() {
        addons.values().forEach(addon -> {
            logger.info("Disabling addon " + addon.getName());
            addon.onDisable();
        });
        addons.values().forEach(this::unregister);
        addons.clear();
    }

    /**
     * Registers an addon in the AddonManager.
     *
     * @param addon The addon to be registered.
     */
    public void register(Addon addon) {
        addons.put(addon.getName(), addon);
    }

    /**
     * Unregisters an addon from the AddonManager.
     *
     * @param addon The addon to be unregistered.
     */
    public void unregister(Addon addon) {
        addons.remove(addon.getName());
    }

    /**
     * Returns a read-only view of the registered addons in the AddonManager.
     *
     * @return A read-only ConcurrentHashMap containing the registered addons.
     */
    public ConcurrentHashMap<String, Addon> getAddons() {
        return new ConcurrentHashMap<>(addons);
    }

    /**
     * Retrieves an addon of the specified type from the loaded addons.
     *
     * @param <T>   The type of the addon to retrieve, extending the Addon class.
     * @param addon The class object representing the type of the addon to be retrieved.
     * @return An instance of the specified addon type if found, or {@code null} if not present.
     */
    @Nullable
    public <T extends Addon> T getAddon(Class<T> addon) {
        return addons.entrySet().parallelStream()
                .map(Map.Entry::getValue)
                .filter(addon::isInstance)
                .map(addon::cast)
                .findFirst()
                .orElse(null);
    }

}
