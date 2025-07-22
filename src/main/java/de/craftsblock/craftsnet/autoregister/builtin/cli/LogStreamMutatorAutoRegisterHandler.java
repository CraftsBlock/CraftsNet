package de.craftsblock.craftsnet.autoregister.builtin.cli;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;
import de.craftsblock.craftsnet.logging.mutate.LogStream;
import de.craftsblock.craftsnet.logging.mutate.LogStreamMutator;

/**
 * A handler for automatically registering {@link LogStreamMutator} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link LogStreamMutator} instances into the log stream of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.5.2
 */
public class LogStreamMutatorAutoRegisterHandler extends AutoRegisterHandler<LogStreamMutator> {

    private final LogStream logStream;

    /**
     * Constructs a new {@link LogStreamMutatorAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the log stream.
     */
    public LogStreamMutatorAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.logStream = craftsNet.getLogStream();
    }

    /**
     * Handles the registration of the provided {@link LogStreamMutator}.
     *
     * <p>This method attempts to register the given {@link LogStreamMutator} with the {@link CraftsNet#getLogStream()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}.</p>
     *
     * @param mutator The {@link LogStreamMutator} to be registered.
     * @param args    Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    @Override
    protected boolean handle(LogStreamMutator mutator, AutoRegisterInfo info, Object... args) {
        if (logStream.isLogStreamMutatorRegistered(mutator)) return false;

        logStream.registerLogStreamMutator(mutator);
        return true;
    }


}
