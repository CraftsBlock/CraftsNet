package de.craftsblock.craftsnet.autoregister.builtin.http;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Handler;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link Handler} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link Handler} instances into the route registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.4
 * @since 3.2.0-SNAPSHOT
 */
public class HandlerAutoRegisterHandler extends AutoRegisterHandler<Handler> {

    private final RouteRegistry routeRegistry;

    /**
     * Constructs a new {@link HandlerAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the route registry.
     */
    public HandlerAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.routeRegistry = craftsNet.getRouteRegistry();
    }

    /**
     * Handles the registration of the provided {@link Handler}.
     *
     * <p>This method attempts to register the given {@link Handler} with the {@link CraftsNet#getRouteRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}.</p>
     *
     * @param handler The {@link Handler} to be registered.
     * @param args    Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    @Override
    protected boolean handle(Handler handler, AutoRegisterInfo info, Object... args) {
        if (routeRegistry.isRegistered(handler)) return false;

        routeRegistry.register(handler);
        return true;
    }

}
