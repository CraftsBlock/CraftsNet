package de.craftsblock.craftsnet.autoregister.builtin.sockets.codec;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.websocket.annotations.ApplyDecoder;
import de.craftsblock.craftsnet.api.websocket.codec.WebSocketSafeTypeDecoder;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link WebSocketSafeTypeDecoder} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete displays a warning due to the fact, that these are only
 * registered using {@link ApplyDecoder}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.5.0
 */
public class WebSocketTypeDecoderAutoRegisterHandler extends AutoRegisterHandler<WebSocketSafeTypeDecoder<?>> {

    /**
     * Constructs an {@link WebSocketTypeDecoderAutoRegisterHandler} with the specified {@link CraftsNet} instance.
     *
     * @param craftsNet The main {@link CraftsNet} instance, which provides access to the application's context.
     */
    public WebSocketTypeDecoderAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
    }

    /**
     * Handles the registration of the provided {@link WebSocketSafeTypeDecoder}.
     *
     * <p>This method is used to display a warning, because {@link WebSocketSafeTypeDecoder}
     * are only registered to a {@link de.craftsblock.craftsnet.api.websocket.SocketHandler}
     * using {@link ApplyDecoder}</p>
     *
     * @param webSocketSafeTypeDecoder The {@link WebSocketSafeTypeDecoder} to be registered.
     * @param args                     Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    @Override
    protected boolean handle(WebSocketSafeTypeDecoder<?> webSocketSafeTypeDecoder, AutoRegisterInfo info, Object... args) {
        craftsNet.getLogger().warning("%s may only be applied using @%s!".formatted(
                webSocketSafeTypeDecoder.getClass().getName(), ApplyDecoder.class.getSimpleName()
        ));
        return true;
    }

}
