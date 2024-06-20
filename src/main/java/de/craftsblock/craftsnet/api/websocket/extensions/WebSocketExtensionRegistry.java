package de.craftsblock.craftsnet.api.websocket.extensions;

import de.craftsblock.craftsnet.api.websocket.extensions.builtin.PerMessageDeflateExtension;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebSocketExtensionRegistry {

    private final ConcurrentHashMap<String, WebSocketExtension> extensions = new ConcurrentHashMap<>();

    public WebSocketExtensionRegistry() {
        register(new PerMessageDeflateExtension());
    }

    public void register(WebSocketExtension extension) {
        extensions.put(extension.getProtocolName(), extension);
    }

    public void unregister(WebSocketExtension extension) {
        this.unregister(extension.getProtocolName());
    }

    public void unregister(String protocolName) {
        extensions.remove(protocolName);
    }

    public boolean hasExtension(WebSocketExtension extension) {
        return hasExtension(extension.getProtocolName());
    }

    public boolean hasExtension(String protocolName) {
        return extensions.containsKey(protocolName);
    }

    public WebSocketExtension getExtensionByName(String protocolName) {
        return extensions.get(protocolName);
    }

    public ConcurrentLinkedQueue<WebSocketExtension> getExtensions() {
        return new ConcurrentLinkedQueue<>(extensions.values());
    }

}
