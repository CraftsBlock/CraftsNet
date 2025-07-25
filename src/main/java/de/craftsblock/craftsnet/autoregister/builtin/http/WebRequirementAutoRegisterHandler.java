package de.craftsblock.craftsnet.autoregister.builtin.http;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.requirements.RequirementRegistry;
import de.craftsblock.craftsnet.api.requirements.web.WebRequirement;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link WebRequirement} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link WebRequirement} instances into the requirement registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.4
 * @since 3.2.0-SNAPSHOT
 */
public class WebRequirementAutoRegisterHandler extends AutoRegisterHandler<WebRequirement> {

    private final RequirementRegistry requirementRegistry;

    /**
     * Constructs a new {@link WebRequirement}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the requirement registry.
     */
    public WebRequirementAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.requirementRegistry = craftsNet.getRequirementRegistry();
    }

    /**
     * Handles the registration of the provided {@link WebRequirement}.
     *
     * <p>This method attempts to register the given {@link WebRequirement} with the {@link CraftsNet#getRequirementRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}.</p>
     *
     * @param webRequirement The {@link WebRequirement} to be registered.
     * @param args           Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    @Override
    protected boolean handle(WebRequirement webRequirement, AutoRegisterInfo info, Object... args) {
        if (requirementRegistry.isRegistered(webRequirement)) return false;

        requirementRegistry.register(webRequirement, true);
        return true;
    }

}
