package de.craftsblock.craftsnet.addon;

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
 * @see Addon
 * @see AddonLoader
 * @since 1.0.0
 */
public class AddonManager {

    private final AddonLoader addonLoader = new AddonLoader();
    private final ConcurrentHashMap<String, Addon> addons = new ConcurrentHashMap<>();

    /**
     * Constructor for the AddonManager class. It loads and initializes the addons present in the "./addons/" folder.
     * It also adds a shutdown hook to handle cleanup when the application is shut down.
     *
     * @throws IOException if there is an I/O error while accessing the addons folder.
     */
    public AddonManager() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        File folder = new File("./addons/");
        if (!folder.isDirectory()) {
            folder.delete();
            folder.mkdirs();
        }
        for (File file : Objects.requireNonNull(folder.listFiles()))
            if (file.getName().endsWith(".jar")) addonLoader.add(file);
        addonLoader.load(this);
    }

    /**
     * Method to stop the AddonManager. It is called during application shutdown.
     */
    public void stop() {
        addons.values().forEach(Addon::onDisable);
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

}
