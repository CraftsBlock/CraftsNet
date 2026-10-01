package de.craftsblock.craftsnet.addon.services.builtin.handlers;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.HttpHandler;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link HttpHandler}.
 * This class specifically focuses on loading instances of {@link HttpHandler} into the {@link RouteRegistry}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.1.0-SNAPSHOT
 */
public class RequestHandlerLoader implements ServiceLoader<HttpHandler> {

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
     * Loads an {@link HttpHandler} into the {@link RouteRegistry} for further processing.
     *
     * @param provider The instance of the {@link HttpHandler} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(HttpHandler provider) {
        craftsNet.getRouteRegistry().register(provider);
        return true;
    }

}