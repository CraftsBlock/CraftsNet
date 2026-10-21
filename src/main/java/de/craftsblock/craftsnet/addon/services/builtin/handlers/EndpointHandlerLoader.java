package de.craftsblock.craftsnet.addon.services.builtin.handlers;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.api.EndpointHandler;
import de.craftsblock.craftsnet.api.RouteRegistry;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link EndpointHandler}.
 * This class specifically focuses on loading instances of {@link EndpointHandler} into the {@link RouteRegistry}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.1.0-SNAPSHOT
 */
public class EndpointHandlerLoader implements ServiceLoader<EndpointHandler> {

    private final CraftsNet craftsNet;

    /**
     * Creates a new instance of {@link EndpointHandlerLoader}.
     *
     * @param craftsNet The instance of {@link CraftsNet} that the {@link EndpointHandlerLoader} was registered on.
     */
    public EndpointHandlerLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Loads an {@link EndpointHandler} into the {@link RouteRegistry} for further processing.
     *
     * @param provider The instance of the {@link EndpointHandler} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(EndpointHandler provider) {
        craftsNet.getRouteRegistry().register(provider);
        return true;
    }

}
