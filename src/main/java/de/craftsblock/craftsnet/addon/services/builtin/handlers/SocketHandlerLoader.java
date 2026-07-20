package de.craftsblock.craftsnet.addon.services.builtin.handlers;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.websocket.WebSocketHandler;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link WebSocketHandler}.
 * This class specifically focuses on loading instances of {@link WebSocketHandler} into the {@link RouteRegistry}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.1.0-SNAPSHOT
 */
public class SocketHandlerLoader implements ServiceLoader<WebSocketHandler> {

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
     * Loads an {@link WebSocketHandler} into the {@link RouteRegistry} for further processing.
     *
     * @param provider The instance of the {@link WebSocketHandler} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(WebSocketHandler provider) {
        craftsNet.getRouteRegistry().register(provider);
        return true;
    }

}