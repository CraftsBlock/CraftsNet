package de.craftsblock.craftsnet.autoregister.buildin;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.meta.Startup;
import de.craftsblock.craftsnet.api.http.body.Body;
import de.craftsblock.craftsnet.api.http.body.BodyParser;
import de.craftsblock.craftsnet.api.http.body.BodyRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link BodyParser} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link BodyParser} instances into the body registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.2.0-SNAPSHOT
 */
public class BodyAutoRegisterHandler extends AutoRegisterHandler<BodyParser<? extends Body>> {

    private final BodyRegistry bodyRegistry;

    /**
     * Constructs a new {@link BodyAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the body registry.
     */
    public BodyAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.bodyRegistry = craftsNet.bodyRegistry();
    }

    /**
     * Handles the registration of the provided {@link BodyParser}.
     *
     * <p>This method attempts to register the given {@link BodyParser} with the {@link CraftsNet#bodyRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}. If any exception occurs during the registration process, a
     * {@link RuntimeException} is thrown.</p>
     *
     * @param bodyParser The {@link BodyParser} to be registered.
     * @param args       Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     * @throws RuntimeException If an error occurs during the registration process.
     */
    @Override
    protected boolean handle(BodyParser<? extends Body> bodyParser, AutoRegisterInfo info, Object... args) {
        try {
            if (bodyRegistry.isRegistered(bodyParser)) return true;
            if (args.length == 1 && info.annotation() instanceof AutoRegister annotation && args[0] instanceof Startup startup
                    && startup != annotation.value()) return false;

            bodyRegistry.register(bodyParser);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
