package de.craftsblock.craftsnet.api.websocket.extensions;

import de.craftsblock.craftsnet.api.websocket.Frame;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for WebSocket extensions. A WebSocket extension allows for
 * custom encoding and decoding of WebSocket frames to support features such as
 * compression, encryption, or other protocol enhancements.
 *
 * <p>Each extension has a protocol name that identifies it. Subclasses must implement
 * the {@link #encode(Frame)} and {@link #decode(Frame)} methods to define how frames
 * are transformed by the extension.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.6-SNAPSHOT
 */
public abstract class WebSocketExtension {

    private final String protocolName;

    /**
     * Constructs a new WebSocket extension with the specified protocol name.
     *
     * @param protocolName the name of the protocol for this extension. This name is
     *                     used to identify the extension during the WebSocket handshake
     *                     and negotiation process.
     */
    public WebSocketExtension(String protocolName) {
        this.protocolName = protocolName;
    }

    /**
     * Encodes a WebSocket frame according to the rules of this extension. This method
     * is called before the frame is sent over the network.
     *
     * @param frame the frame to be encoded. The frame should not be null.
     * @return the encoded frame. The result should not be null.
     */
    public abstract @NotNull Frame encode(@NotNull Frame frame);

    /**
     * Decodes a WebSocket frame according to the rules of this extension. This method
     * is called after the frame is received from the network.
     *
     * @param frame the frame to be decoded. The frame should not be null.
     * @return the decoded frame. The result should not be null.
     */
    public abstract @NotNull Frame decode(@NotNull Frame frame);

    /**
     * Returns the protocol name of this extension.
     *
     * @return the protocol name. This value is used to identify the extension.
     */
    public final String getProtocolName() {
        return protocolName;
    }

}
