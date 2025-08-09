package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.loaders.AddonClassLoader;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonMeta;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.codec.registry.TypeEncoderRegistry;
import de.craftsblock.craftsnet.api.http.body.BodyRegistry;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoderRegistry;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareRegistry;
import de.craftsblock.craftsnet.api.requirements.RequirementRegistry;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeEncoder;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtensionRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterRegistry;
import de.craftsblock.craftsnet.command.CommandRegistry;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.logging.mutate.LogStream;
import de.craftsblock.craftsnet.utils.FileHelper;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
 * @version 1.3.2
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
     * @deprecated Use {@link #getCraftsNet()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final CraftsNet craftsNet() {
        return craftsNet;
    }

    /**
     * Get the CraftsNet instance on which the addon was registered.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The CraftsNet instance on which the addon was registered.
     * @since 3.1.0-SNAPSHOT
     */
    public CraftsNet getCraftsNet() {
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
     * Retrieves the {@link AddonManager} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying it directly.
     *
     * @return The {@link AddonManager} instance used by the addon.
     * @since 3.1.0-SNAPSHOT
     */
    public final AddonManager getAddonManager() {
        return craftsNet.getAddonManager();
    }

    /**
     * Retrieves the {@link AutoRegisterRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link AutoRegisterRegistry} instance used by the addon.
     * @since 3.2.0-SNAPSHOT
     * @deprecated Use {@link #getAutoRegisterRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final AutoRegisterRegistry autoRegisterRegistry() {
        return this.getAutoRegisterRegistry();
    }

    /**
     * Retrieves the {@link AutoRegisterRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link AutoRegisterRegistry} instance used by the addon.
     * @since 3.2.0-SNAPSHOT
     */
    public final AutoRegisterRegistry getAutoRegisterRegistry() {
        return craftsNet.getAutoRegisterRegistry();
    }

    /**
     * Get the {@link BodyRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link BodyRegistry} instance used by the addon.
     * @since 3.0.4-SNAPSHOT
     * @deprecated Use {@link #getBodyRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final BodyRegistry bodyRegistry() {
        return this.getBodyRegistry();
    }

    /**
     * Get the {@link BodyRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link BodyRegistry} instance used by the addon.
     * @since 3.0.4-SNAPSHOT
     */
    public final BodyRegistry getBodyRegistry() {
        return craftsNet.getBodyRegistry();
    }

    /**
     * Get the {@link CommandRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link CommandRegistry} instance used by the addon.
     * @deprecated Use {@link #getCommandRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final CommandRegistry commandRegistry() {
        return this.getCommandRegistry();
    }

    /**
     * Get the {@link CommandRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link CommandRegistry} instance used by the addon.
     */
    public final CommandRegistry getCommandRegistry() {
        return craftsNet.getCommandRegistry();
    }

    /**
     * Get the {@link FileHelper} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying it directly.
     *
     * @return The {@link FileHelper} instance used by the addon.
     */
    public final FileHelper getFileHelper() {
        return craftsNet.getFileHelper();
    }

    /**
     * Get the {@link ListenerRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link ListenerRegistry} instance used by the addon.
     * @deprecated Use {@link #getListenerRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final ListenerRegistry listenerRegistry() {
        return this.getListenerRegistry();
    }

    /**
     * Get the {@link ListenerRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link ListenerRegistry} instance used by the addon.
     */
    public final ListenerRegistry getListenerRegistry() {
        return craftsNet.getListenerRegistry();
    }

    /**
     * Get the {@link LogStream} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying it directly.
     *
     * @return The {@link LogStream} instance used by the addon.
     */
    public final LogStream getLogStream() {
        return craftsNet.getLogStream();
    }

    /**
     * Get the {@link MiddlewareRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link MiddlewareRegistry} instance used by the addon.
     * @since 3.4.0-SNAPSHOT
     * @deprecated Use {@link #getMiddlewareRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final MiddlewareRegistry middlewareRegistry() {
        return this.getMiddlewareRegistry();
    }

    /**
     * Get the {@link MiddlewareRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link MiddlewareRegistry} instance used by the addon.
     * @since 3.4.0-SNAPSHOT
     */
    public final MiddlewareRegistry getMiddlewareRegistry() {
        return craftsNet.getMiddlewareRegistry();
    }

    /**
     * Get the {@link RequirementRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link RequirementRegistry} instance used by the addon.
     * @since 3.2.1-SNAPSHOT
     * @deprecated Use {@link #getRequirementRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final RequirementRegistry requirementRegistry() {
        return this.getRequirementRegistry();
    }

    /**
     * Get the {@link RequirementRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the registry directly.
     *
     * @return The {@link RequirementRegistry} instance used by the addon.
     * @since 3.2.1-SNAPSHOT
     */
    public final RequirementRegistry getRequirementRegistry() {
        return craftsNet.getRequirementRegistry();
    }

    /**
     * Get the {@link RouteRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link RouteRegistry} instance used by the addon.
     * @deprecated Use {@link #getRouteRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final RouteRegistry routeRegistry() {
        return this.getRouteRegistry();
    }

    /**
     * Get the {@link RouteRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link RouteRegistry} instance used by the addon.
     */
    public final RouteRegistry getRouteRegistry() {
        return craftsNet.getRouteRegistry();
    }

    /**
     * Get the {@link ServiceManager} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link ServiceManager} instance used by the addon.
     * @deprecated Use {@link #getServiceManager()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final ServiceManager serviceManager() {
        return this.getServiceManager();
    }

    /**
     * Get the {@link ServiceManager} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link ServiceManager} instance used by the addon.
     */
    public final ServiceManager getServiceManager() {
        return craftsNet.getServiceManager();
    }

    /**
     * Get the {@link StreamEncoderRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link StreamEncoderRegistry} instance used by the addon.
     * @since 3.3.3-SNAPSHOT
     * @deprecated Use {@link #getStreamEncoderRegistry()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final StreamEncoderRegistry streamEncoderRegistry() {
        return this.getStreamEncoderRegistry();
    }

    /**
     * Get the {@link StreamEncoderRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the handler directly.
     *
     * @return The {@link StreamEncoderRegistry} instance used by the addon.
     * @since 3.3.3-SNAPSHOT
     */
    public final StreamEncoderRegistry getStreamEncoderRegistry() {
        return craftsNet.getStreamEncoderRegistry();
    }

    /**
     * Retrieves the {@link TypeEncoderRegistry} dedicated to managing
     * {@link WebSocketSafeTypeEncoder} instances used by the {@link de.craftsblock.craftsnet.api.websocket.WebSocketServer}.
     *
     * @return the {@link TypeEncoderRegistry} for {@link WebSocketSafeTypeEncoder} codecs
     */
    public final TypeEncoderRegistry<WebSocketSafeTypeEncoder<?, ?>> getWebSocketEncoderRegistry() {
        return craftsNet.getWebSocketEncoderRegistry();
    }

    /**
     * Retrieves the {@link WebSocketExtensionRegistry} instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying it directly.
     *
     * @return The {@link WebSocketExtensionRegistry} instance used by the addon.
     * @since 3.5.0
     */
    public final WebSocketExtensionRegistry getWebsocketExtensionRegistry() {
        return craftsNet.getWebSocketExtensionRegistry();
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
     * @deprecated Use {@link #getLogger()} instead. This will be removed in the future.
     */
    @Deprecated(since = "3.5.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
    public final Logger logger() {
        return this.getLogger();
    }

    /**
     * Get the logger instance used by the addon.
     * This method is marked as final to prevent subclasses from modifying the logger directly.
     *
     * @return The logger instance used by the addon.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Retrieves the data folder for this addon as a {@link File} object.
     * <p>
     * The folder location is determined by {@link #getDataPath()}, which ensures
     * the directory exists before returning it. This method is a convenience
     * wrapper to convert the {@link Path} into a {@link File}.
     *
     * @return The data folder for this addon as a {@link File}
     */
    public final File getDataFolder() {
        return getDataPath().toFile();
    }

    /**
     * Retrieves the data directory for this addon as a {@link Path}.
     * <p>
     * The path is resolved relative to the {@code addons} directory, using the
     * addon's name as the subdirectory name. If the directory does not exist,
     * it will be created. If the resolved path exists but is not a directory,
     * an {@link IllegalStateException} will be thrown.
     *
     * @return The data path for this add-on
     * @throws RuntimeException      If the data path cannot be created or accessed
     * @throws IllegalStateException If the resolved path exists but is not a directory
     * @since 3.5.2
     */
    public final Path getDataPath() {
        try {
            Path path = Path.of("addons", getName());

            if (!Files.exists(path)) Files.createDirectories(path);
            if (!Files.isDirectory(path))
                throw new IllegalStateException("The path %s must be an directory!".formatted(
                        path.toAbsolutePath()
                ));

            return path;
        } catch (IOException e) {
            throw new RuntimeException("Could not load the data path!", e);
        }
    }

    /**
     * Retrieves an addon of the specified type from the loaded addons.
     *
     * @param <T>   The type of the addon to retrieve, extending the Addon class.
     * @param addon The class object representing the type of the addon to be retrieved.
     * @return An instance of the specified addon type if found, or {@code null} if not present.
     */
    public <T extends Addon> T getAddon(Class<T> addon) {
        return craftsNet.getAddonManager().getAddon(addon);
    }

    /**
     * Retrieves an addon by its name from the loaded addons.
     *
     * @param <T>  The type of the addon to retrieve, extending the Addon class.
     * @param name The name of the addon to be retrieved.
     * @return An instance of the specified addon type if found, or {@code null} if not present.
     * @since 3.3.5-SNAPSHOT
     */
    public <T extends Addon> T getAddon(String name) {
        return craftsNet.getAddonManager().getAddon(name);
    }

}
