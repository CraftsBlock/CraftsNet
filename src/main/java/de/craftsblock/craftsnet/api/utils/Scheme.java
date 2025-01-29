package de.craftsblock.craftsnet.api.utils;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.exceptions.UnknownSchemeException;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import org.jetbrains.annotations.NotNull;

/**
 * Enum representing different schemes (protocols) supported by the system.
 * <p>
 * The {@code Scheme} enum defines various network protocols such as {@code HTTP}, {@code HTTPS}, {@code WS}, and {@code WSS}.
 * Each scheme is associated with a specific server type. The scheme can be used to determine the appropriate server
 * to handle network requests, whether it be an HTTP server or WebSocket server.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.2-SNAPSHOT
 */
public enum Scheme {

    /**
     * HTTP protocol scheme, associated with a {@link WebServer}.
     */
    HTTP("http", WebServer.class),

    /**
     * HTTPS protocol scheme, associated with a {@link WebServer}.
     */
    HTTPS("https", WebServer.class),

    /**
     * WebSocket protocol scheme, associated with a {@link WebSocketServer}.
     */
    WS("ws", WebSocketServer.class),

    /**
     * Secure WebSocket protocol scheme, associated with a {@link WebSocketServer}.
     */
    WSS("wss", WebSocketServer.class);

    private final String name;
    private final Class<? extends Server> server;

    /**
     * Constructs a new {@code Scheme} enum instance.
     *
     * @param name   The string representation of the scheme (e.g., "http", "ws").
     * @param server The server class associated with the scheme.
     */
    Scheme(String name, Class<? extends Server> server) {
        this.name = name;
        this.server = server;
    }

    /**
     * Gets the name of the scheme.
     *
     * @return A string representing the scheme name (e.g., "http", "wss").
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the scheme with or without SSL support.
     * <p>
     * If SSL is requested, it returns the HTTPS or WSS variant; otherwise, it returns the HTTP or WS variant.
     * </p>
     *
     * @param ssl A boolean indicating whether SSL is enabled or not.
     * @return The corresponding {@link Scheme} with or without SSL.
     */
    public Scheme getSsl(boolean ssl) {
        return switch (this) {
            case HTTP, HTTPS -> ssl ? HTTPS : HTTP;
            case WS, WSS -> ssl ? WSS : WS;
        };
    }

    /**
     * Retrieves the appropriate server instance for the current scheme.
     *
     * @param craftsNet The {@link CraftsNet} instance used to access the servers.
     * @return The server instance associated with the current scheme.
     * @throws UnknownSchemeException If no server instance is found for the current scheme.
     */
    public @NotNull Server getServer(@NotNull CraftsNet craftsNet) {
        if (this.server.equals(WebServer.class)) return craftsNet.webServer();
        if (this.server.equals(WebSocketServer.class)) return craftsNet.webSocketServer();
        throw new UnknownSchemeException("No server instance for scheme (" + this.name() + ") found!");
    }

    /**
     * Gets the raw server class type associated with the current scheme.
     *
     * @return The class type of the server associated with the current scheme.
     */
    public Class<? extends Server> getServerRaw() {
        return server;
    }

}
