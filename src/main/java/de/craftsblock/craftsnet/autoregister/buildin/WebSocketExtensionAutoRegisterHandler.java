package de.craftsblock.craftsnet.autoregister.buildin;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.meta.Startup;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtension;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtensionRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link WebSocketExtension} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link WebSocketExtension} instances into the websocket extension registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.2.0-SNAPSHOT
 */
public class WebSocketExtensionAutoRegisterHandler extends AutoRegisterHandler<WebSocketExtension> {

    private final WebSocketExtensionRegistry webSocketExtensionRegistry;

    /**
     * Constructs a new {@link WebSocketExtensionAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the websocket extension registry.
     */
    public WebSocketExtensionAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.webSocketExtensionRegistry = craftsNet.webSocketExtensionRegistry();
    }

    /**
     * Handles the registration of the provided {@link WebSocketExtension}.
     *
     * <p>This method attempts to register the given {@link WebSocketExtension} with the {@link CraftsNet#webSocketExtensionRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}. If any exception occurs during the registration process, a
     * {@link RuntimeException} is thrown.</p>
     *
     * @param extension The {@link WebSocketExtension} to be registered.
     * @param args      Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     * @throws RuntimeException If an error occurs during the registration process.
     */
    @Override
    protected boolean handle(WebSocketExtension extension, AutoRegisterInfo info, Object... args) {
        try {
            if (webSocketExtensionRegistry.hasExtension(extension)) return true;
            if (args.length == 1 && info.annotation() instanceof AutoRegister annotation && args[0] instanceof Startup startup
                    && startup != annotation.value()) return false;

            webSocketExtensionRegistry.register(extension);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}