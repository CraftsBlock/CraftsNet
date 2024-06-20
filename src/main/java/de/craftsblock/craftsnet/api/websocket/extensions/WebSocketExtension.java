package de.craftsblock.craftsnet.api.websocket.extensions;

import de.craftsblock.craftsnet.api.websocket.Frame;
import org.jetbrains.annotations.NotNull;

public abstract class WebSocketExtension {

    private final String protocolName;

    public WebSocketExtension(String protocolName) {
        this.protocolName = protocolName;
    }

    public abstract @NotNull Frame encode(@NotNull Frame frame);

    public abstract @NotNull Frame decode(@NotNull Frame frame);

    public final String getProtocolName() {
        return protocolName;
    }

}
