package de.craftsblock.craftsnet.autoregister.builtin.sockets.codec;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.codec.registry.TypeEncoderRegistry;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeEncoder;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link WebSocketSafeTypeEncoder} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link WebSocketSafeTypeEncoder} instances into the websocket encoder registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.5.0
 */
public class WebSocketTypeEncoderAutoRegisterHandler extends AutoRegisterHandler<WebSocketSafeTypeEncoder<?, ?>> {

    private final TypeEncoderRegistry<WebSocketSafeTypeEncoder<?, ?>> encoderRegistry;

    /**
     * Constructs an {@link WebSocketTypeEncoderAutoRegisterHandler} with the specified {@link CraftsNet} instance.
     *
     * @param craftsNet The main {@link CraftsNet} instance, which provides access to the application's context.
     */
    public WebSocketTypeEncoderAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.encoderRegistry = craftsNet.getWebSocketEncoderRegistry();
    }

    /**
     * Handles the registration of the provided {@link WebSocketSafeTypeEncoder}.
     *
     * <p>This method attempts to register the given {@link WebSocketSafeTypeEncoder} with the
     * {@link CraftsNet#getWebSocketEncoderRegistry()} of the associated {@link CraftsNet} instance.
     * If registration is successful, the method returns {@code true}.</p>
     *
     * @param webSocketSafeTypeEncoder The {@link WebSocketSafeTypeEncoder} to be registered.
     * @param args                     Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     * @throws RuntimeException If an error occurs during the registration process.
     */
    @Override
    protected boolean handle(WebSocketSafeTypeEncoder<?, ?> webSocketSafeTypeEncoder, AutoRegisterInfo info, Object... args) {
        if (encoderRegistry.isRegistered(webSocketSafeTypeEncoder)) return false;

        encoderRegistry.register(webSocketSafeTypeEncoder);
        return true;
    }

}
