package de.craftsblock.craftsnet.addon.services.builtin.handlers;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.RequestHandler;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link RequestHandler}.
 * This class specifically focuses on loading instances of {@link RequestHandler} into the {@link RouteRegistry}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class RequestHandlerLoader implements ServiceLoader<RequestHandler> {

    private final CraftsNet craftsNet;

    /**
     * Creates a new instance of {@link RequestHandlerLoader}.
     *
     * @param craftsNet The instance of {@link CraftsNet} that the {@link RequestHandlerLoader} was registered on.
     */
    public RequestHandlerLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Loads an {@link RequestHandler} into the {@link RouteRegistry} for further processing.
     *
     * @param provider The instance of the {@link RequestHandler} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(RequestHandler provider) {
        craftsNet.routeRegistry().register(provider);
        return true;
    }

}