package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.loaders.AddonClassLoader;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonMeta;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.body.BodyRegistry;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoderRegistry;
import de.craftsblock.craftsnet.api.requirements.RequirementRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterRegistry;
import de.craftsblock.craftsnet.command.CommandRegistry;
import de.craftsblock.craftsnet.logging.Logger;

import java.io.File;

/**
 * Abstract class representing an addon that extends the functionality of the application without modifying its core.
 * Addons can be loaded, enabled, and disabled to provide additional features or customizations to the application.
 *
 * <p>The {@code Addon} class serves as a base for creating custom addons. Subclasses of {@code Addon} must
 * implement the {@link #onEnable()} and {@link #onDisable()} methods to define the behavior when the addon is
 * enabled or disabled, respectively. The {@link #onLoad()} method can be optionally overridden to perform actions
 * during addon loading.</p>
 *
 * <p>Each addon has access to a {@link RouteRegistry} instance for handling routes and a {@link ListenerRegistry}
 * instance for registering listeners within the addon. It also has a {@link Logger} instance for logging messages
 * specific to the addon and a {@link ServiceManager} for registering service loaders.</p>
 *
 * <p>The {@code Addon} class provides a convenient way to extend the application's capabilities through custom
 * functionality, making it easy to add new features or modify existing ones without directly altering the application's
 * core code.</p>
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.2
 * @see AddonLoader
 * @see AddonManager
 * @since 1.0.0-SNAPSHOT
 */
public abstract class Addon {

    private CraftsNet craftsNet;
    private String name;
    private AddonMeta meta;

    private AddonClassLoader classLoader;

    private Logger logger;

    /**
     * Called when the addon is being loaded.
     * This method can be overridden to perform any setup or initialization tasks.
     */
    public void onLoad() {
    }

    /**
     * Called when the addon is being enabled.
     * This method must be implemented by subclasses to define the addon's functionality.
     */
    public void onEnable() {
    }

    /**
     * Called when the addon is being disabled.
     * This method must be implemented by subclasses to clean up any resources or perform
     * any necessary shutdown tasks.
     */
    public void onDisable() {
    }

    /**
     * Get the CraftsNet instance on which the addon was registered.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The CraftsNet instance on which the addon was registered.
     * @since 3.1.0-SNAPSHOT
     */
    public final CraftsNet craftsNet() {
        return craftsNet;
    }

    /**
     * Get the name of the addon.
     *
     * @return The name of the addon.
     */
    public final String getName() {
        return meta.name();
    }

    /**
     * Get the metadata of the addon.
     *
     * @return The metadata of the addon.
     * @since 3.1.0-SNAPSHOT
     */
    public AddonMeta getMeta() {
        return meta;
    }

    /**
     * Retrieves the {@link AutoRegisterRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link AutoRegisterRegistry} instance used by the addon.
     * @since 3.2.0-SNAPSHOT
     */
    public final AutoRegisterRegistry autoRegisterRegistry() {
        return craftsNet.autoRegisterRegistry();
    }

    /**
     * Get the {@link BodyRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link BodyRegistry} instance used by the addon.
     * @since 3.0.4-SNAPSHOT
     */
    public final BodyRegistry bodyRegistry() {
        return craftsNet.bodyRegistry();
    }

    /**
     * Get the {@link CommandRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link CommandRegistry} instance used by the addon.
     */
    public final CommandRegistry commandRegistry() {
        return craftsNet.commandRegistry();
    }

    /**
     * Get the {@link ListenerRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link ListenerRegistry} instance used by the addon.
     */
    public final ListenerRegistry listenerRegistry() {
        return craftsNet.listenerRegistry();
    }

    /**
     * Get the {@link RequirementRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link RequirementRegistry} instance used by the addon.
     * @since 3.2.1-SNAPSHOT
     */
    public final RequirementRegistry requirementRegistry() {
        return craftsNet.requirementRegistry();
    }

    /**
     * Get the {@link RouteRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link RouteRegistry} instance used by the addon.
     */
    public final RouteRegistry routeRegistry() {
        return craftsNet.routeRegistry();
    }

    /**
     * Get the {@link ServiceManager} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link ServiceManager} instance used by the addon.
     */
    public final ServiceManager serviceManager() {
        return craftsNet.serviceManager();
    }

    /**
     * Get the {@link StreamEncoderRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link StreamEncoderRegistry} instance used by the addon.
     * @since 3.3.3-SNAPSHOT
     */
    public final StreamEncoderRegistry streamEncoderRegistry() {
        return craftsNet.streamEncoderRegistry();
    }

    /**
     * Get the class loader instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The class loader used by this addon.
     */
    public final ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Get the logger instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the logger directly.
     *
     * @return The logger instance used by the addon.
     */
    public final Logger logger() {
        return logger;
    }

    /**
     * Get the data folder for the addon.
     * The data folder is a directory specific to each addon where it can store any files or data it needs.
     * If the data folder does not exist, it will be created.
     * If the data folder exists but is not a directory, it will be deleted, and a new one will be created.
     *
     * @return The data folder for the addon.
     */
    public final File getDataFolder() {
        File folder = new File("./addons/" + getName() + "/");
        if (!folder.exists())
            folder.mkdirs();
        else if (folder.exists() && !folder.isDirectory()) {
            folder.delete();
            folder = getDataFolder();
        }
        return folder;
    }

    /**
     * Retrieves an addon of the specified type from the loaded addons.
     *
     * @param <T>   The type of the addon to retrieve, extending the Addon class.
     * @param addon The class object representing the type of the addon to be retrieved.
     * @return An instance of the specified addon type if found, or {@code null} if not present.
     */
    public <T extends Addon> T getAddon(Class<T> addon) {
        return craftsNet.addonManager().getAddon(addon);
    }

}
