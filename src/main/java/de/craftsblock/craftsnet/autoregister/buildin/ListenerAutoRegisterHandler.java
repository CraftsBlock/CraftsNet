package de.craftsblock.craftsnet.autoregister.buildin;

import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;

/**
 * A handler for automatically registering {@link ListenerAdapter} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link ListenerAdapter} instances into the listener registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.2.0-SNAPSHOT
 */
public class ListenerAutoRegisterHandler extends AutoRegisterHandler<ListenerAdapter> {

    /**
     * Constructs a new {@link ListenerAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the listener registry.
     */
    public ListenerAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
    }

    /**
     * Handles the registration of the provided {@link ListenerAdapter}.
     *
     * <p>This method attempts to register the given {@link ListenerAdapter} with the {@link CraftsNet#listenerRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}. If any exception occurs during the registration process, a
     * {@link RuntimeException} is thrown.</p>
     *
     * @param adapter The {@link ListenerAdapter} to be registered.
     * @param args    Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     * @throws RuntimeException If an error occurs during the registration process.
     */
    @Override
    protected boolean handle(ListenerAdapter adapter, Object... args) {
        try {
            craftsNet.listenerRegistry().register(adapter);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
