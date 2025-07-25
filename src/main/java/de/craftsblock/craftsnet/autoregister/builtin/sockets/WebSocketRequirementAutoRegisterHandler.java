package de.craftsblock.craftsnet.autoregister.builtin.sockets;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.requirements.RequirementRegistry;
import de.craftsblock.craftsnet.api.requirements.websocket.WebSocketRequirement;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link WebSocketRequirement} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link WebSocketRequirement} instances into the requirement registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.4
 * @since 3.2.0-SNAPSHOT
 */
public class WebSocketRequirementAutoRegisterHandler extends AutoRegisterHandler<WebSocketRequirement<? extends RequireAble>> {

    private final RequirementRegistry requirementRegistry;

    /**
     * Constructs a new {@link WebSocketExtensionAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the requirement registry.
     */
    public WebSocketRequirementAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.requirementRegistry = craftsNet.getRequirementRegistry();
    }

    /**
     * Handles the registration of the provided {@link WebSocketRequirement}.
     *
     * <p>This method attempts to register the given {@link WebSocketRequirement} with the {@link CraftsNet#getRequirementRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}.</p>
     *
     * @param webSocketRequirement The {@link WebSocketRequirement} to be registered.
     * @param args                 Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    @Override
    protected boolean handle(WebSocketRequirement<? extends RequireAble> webSocketRequirement, AutoRegisterInfo info, Object... args) {
        if (requirementRegistry.isRegistered(webSocketRequirement)) return false;

        requirementRegistry.register(webSocketRequirement, true);
        return true;
    }

}
