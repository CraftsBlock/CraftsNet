package de.craftsblock.craftsnet.autoregister.builtin.http;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoderRegistry;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link StreamEncoder} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link StreamEncoder} instances into the stream encoder registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.4
 * @since 3.3.3-SNAPSHOT
 */
public class StreamEncoderAutoRegisterHandler extends AutoRegisterHandler<StreamEncoder> {

    private final StreamEncoderRegistry streamEncoderRegistry;

    /**
     * Constructs a new {@link StreamEncoderAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the body registry.
     */
    public StreamEncoderAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.streamEncoderRegistry = craftsNet.getStreamEncoderRegistry();
    }

    /**
     * Handles the registration of the provided {@link StreamEncoder}.
     *
     * <p>This method attempts to register the given {@link StreamEncoder} with the {@link CraftsNet#getStreamEncoderRegistry()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}.</p>
     *
     * @param streamEncoder The {@link StreamEncoder} to be registered.
     * @param args          Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    @Override
    protected boolean handle(StreamEncoder streamEncoder, AutoRegisterInfo info, Object... args) {
        if (streamEncoderRegistry.isRegistered(streamEncoder)) return false;

        streamEncoderRegistry.register(streamEncoder);
        return true;
    }

}
