package de.craftsblock.craftsnet.autoregister.builtin;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.middlewares.Middleware;
import de.craftsblock.craftsnet.api.middlewares.MiddlewareRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link Middleware} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link Middleware} instances into the requirement registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.4.0-SNAPSHOT
 */
public class MiddlewareAutoRegisterHandler extends AutoRegisterHandler<Middleware> {

    private final MiddlewareRegistry middlewareRegistry;

    /**
     * Constructs a new {@link MiddlewareAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the middleware registry.
     */
    public MiddlewareAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.middlewareRegistry = craftsNet.getMiddlewareRegistry();
    }

    /**
     * Handles the registration of the provided {@link Middleware}.
     *
     * <p>This method attempts to register the given {@link Middleware} with the {@link CraftsNet#getMiddlewareRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}. If any exception occurs during the registration process, a
     * {@link RuntimeException} is thrown.</p>
     *
     * @param middleware The {@link Middleware} to be registered.
     * @param args       Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     * @throws RuntimeException If an error occurs during the registration process.
     */
    @Override
    protected boolean handle(Middleware middleware, AutoRegisterInfo info, Object... args) {
        try {
            if (middlewareRegistry.isRegistered(middleware)) return false;

            middlewareRegistry.register(middleware);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
