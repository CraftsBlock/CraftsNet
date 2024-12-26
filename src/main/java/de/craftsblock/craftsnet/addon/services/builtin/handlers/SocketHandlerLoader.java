package de.craftsblock.craftsnet.addon.services.builtin.handlers;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.SocketHandler;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link SocketHandler}.
 * This class specifically focuses on loading instances of {@link SocketHandler} into the {@link RouteRegistry}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.1.0-SNAPSHOT
 */
public class SocketHandlerLoader implements ServiceLoader<SocketHandler> {

    private final CraftsNet craftsNet;

    /**
     * Creates a new instance of {@link SocketHandlerLoader}.
     *
     * @param craftsNet The instance of {@link CraftsNet} that the {@link SocketHandlerLoader} was registered on.
     */
    public SocketHandlerLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Loads an {@link SocketHandler} into the {@link RouteRegistry} for further processing.
     *
     * @param provider The instance of the {@link SocketHandler} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(SocketHandler provider) {
        craftsNet.routeRegistry().register(provider);
        return true;
    }

}