package de.craftsblock.craftsnet.addon.services.builtin.handlers;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.api.middlewares.Middleware;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareRegistry;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link Middleware}.
 * This class specifically focuses on loading instances of {@link Middleware} into the {@link MiddlewareRegistry}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.3.6-SNAPSHOT
 */
public class MiddlewareHandlerLoader implements ServiceLoader<Middleware> {

    private final CraftsNet craftsNet;

    /**
     * Creates a new instance of {@link MiddlewareHandlerLoader}.
     *
     * @param craftsNet The instance of {@link CraftsNet} that the {@link MiddlewareHandlerLoader} was registered on.
     */
    public MiddlewareHandlerLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Loads an {@link Middleware} into the {@link MiddlewareRegistry} for further processing.
     *
     * @param provider The instance of the {@link Middleware} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(Middleware provider) {
        craftsNet.middlewareRegistry().register(provider);
        return true;
    }

}
