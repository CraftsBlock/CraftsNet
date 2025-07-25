package de.craftsblock.craftsnet.addon.services.builtin.handlers;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.RouteRegistry;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link Handler}.
 * This class specifically focuses on loading instances of {@link Handler} into the {@link RouteRegistry}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.1.0-SNAPSHOT
 */
public class GenericHandlerLoader implements ServiceLoader<Handler> {

    private final CraftsNet craftsNet;

    /**
     * Creates a new instance of {@link GenericHandlerLoader}.
     *
     * @param craftsNet The instance of {@link CraftsNet} that the {@link GenericHandlerLoader} was registered on.
     */
    public GenericHandlerLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Loads an {@link Handler} into the {@link RouteRegistry} for further processing.
     *
     * @param provider The instance of the {@link Handler} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(Handler provider) {
        craftsNet.getRouteRegistry().register(provider);
        return true;
    }

}
