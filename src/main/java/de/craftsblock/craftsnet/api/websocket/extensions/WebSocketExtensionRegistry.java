package de.craftsblock.craftsnet.api.websocket.extensions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Registry for managing WebSocket extensions. This class allows for the registration,
 * unregistration, and lookup of WebSocket extensions by their protocol names. It is designed
 * to be thread-safe, using a concurrent hash map to store the extensions.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.0.6-SNAPSHOT
 */
public class WebSocketExtensionRegistry {

    private final ConcurrentHashMap<String, WebSocketExtension> extensions = new ConcurrentHashMap<>();

    /**
     * Constructs a new WebSocket extension registry and registers the default extensions.
     */
    public WebSocketExtensionRegistry() {
//        register(new PerMessageDeflateExtension());
    }

    /**
     * Registers a new WebSocket extension. If an extension with the same protocol name
     * already exists, it will be replaced.
     *
     * @param extension the WebSocket extension to be registered. Must not be null.
     */
    public void register(WebSocketExtension extension) {
        if (hasExtension(extension)) return;
        extensions.put(extension.getProtocolName(), extension);
    }

    /**
     * Unregisters a WebSocket extension by its instance. This method calls {@link #unregister(String)}
     * with the protocol name of the given extension.
     *
     * @param extension the WebSocket extension to be unregistered. Must not be null.
     */
    public void unregister(WebSocketExtension extension) {
        this.unregister(extension.getProtocolName());
    }

    /**
     * Unregisters a WebSocket extension by its protocol name. If no extension with the given
     * protocol name is registered, this method does nothing.
     *
     * @param protocolName the protocol name of the WebSocket extension to be unregistered. Must not be null.
     */
    public void unregister(String protocolName) {
        extensions.remove(protocolName);
    }

    /**
     * Checks if a WebSocket extension is registered by its instance. This method calls
     * {@link #hasExtension(String)} with the protocol name of the given extension.
     *
     * @param extension the WebSocket extension to check. Must not be null.
     * @return {@code true} if the extension is registered, {@code false} otherwise.
     */
    public boolean hasExtension(WebSocketExtension extension) {
        return hasExtension(extension.getProtocolName());
    }

    /**
     * Checks if a WebSocket extension is registered by its protocol name.
     *
     * @param protocolName the protocol name of the WebSocket extension to check. Must not be null.
     * @return {@code true} if the extension is registered, {@code false} otherwise.
     */
    public boolean hasExtension(String protocolName) {
        return extensions.containsKey(protocolName);
    }

    /**
     * Retrieves a WebSocket extension by its protocol name.
     *
     * @param protocolName the protocol name of the WebSocket extension to retrieve. Must not be null.
     * @return the WebSocket extension associated with the given protocol name, or {@code null} if no such extension is registered.
     */
    public WebSocketExtension getExtensionByName(String protocolName) {
        return extensions.get(protocolName);
    }

    /**
     * Retrieves all registered WebSocket extensions.
     *
     * @return a concurrent linked queue containing all registered WebSocket extensions.
     */
    public ConcurrentLinkedQueue<WebSocketExtension> getExtensions() {
        return new ConcurrentLinkedQueue<>(extensions.values());
    }

}
