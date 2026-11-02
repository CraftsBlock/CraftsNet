package de.craftsblock.craftsnet.api;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.builder.server.ServerBuilder;
import de.craftsblock.craftsnet.builder.server.ServerState;
import de.craftsblock.craftsnet.logging.Logger;

import java.util.Collection;
import java.util.List;

/**
 * Abstract class representing a server in the CraftsNet framework.
 *
 * @param <B> The type of {@link ServerBuilder} used to configure and build this server.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.3-SNAPSHOT
 */
public abstract class Server<B extends ServerBuilder<B>> {

    /**
     * All known server types.
     *
     * @since 3.5.3
     */
    public static final Collection<Class<? extends Server<?>>> SERVER_TYPES = List.of(
            WebServer.class, WebSocketServer.class
    );

    private final B builder;
    private final CraftsNet craftsNet;

    protected int port;
    protected int backlog;
    protected boolean ssl;

    protected boolean running;

    /**
     * Constructs a server with the specified port, backlog, and SSL configuration.
     *
     * @param craftsNet The {@link CraftsNet} instance which instantiates this server.
     * @param builder   The {@link B Builder} which should build this server.
     */
    public Server(CraftsNet craftsNet, B builder) {
        this.craftsNet = craftsNet;
        this.builder = builder;

        this.bind(builder.port(), builder.backlog());
        this.ssl = builder.ssl();
        this.running = false;
    }

    /**
     * Binds the server to the specified port.
     *
     * @param port The port number to bind the server to.
     */
    public void bind(int port) {
        this.bind(port, 0);
    }

    /**
     * Binds the server to the specified port and backlog.
     *
     * @param port    The port number to bind the server to.
     * @param backlog The maximum number of pending connections the server's socket may have in the queue.
     */
    public void bind(int port, int backlog) {
        this.port = port;
        this.backlog = backlog;
    }

    /**
     * Sets whether SSL should be used for the server.
     *
     * @param ssl {@code true} if SSL should be used, {@code false} otherwise.
     */
    public void shouldUseSSL(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * Starts the server.
     */
    public void start() {
        running = true;
    }

    /**
     * Stops the server.
     */
    public void stop() {
        running = false;
    }

    /**
     * Perform necessary actions when the server is awakened or warn if needed.
     */
    public abstract void awakeOrWarn();

    /**
     * Puts the server to sleep if it is not needed.
     */
    public abstract void sleepIfNotNeeded();

    /**
     * Checks if the server is enabled.
     *
     * @return true if the server is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return this.getBuilder().state().isEnabled();
    }

    /**
     * Checks if the server is running.
     *
     * @return true if the server is running, false otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Retrieves the port number the server is bound to.
     *
     * @return The port number of the server.
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves the backlog size of the server.
     *
     * @return The backlog size of the server.
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Checks if SSL is enabled for the server.
     *
     * @return {@code true} if SSL is enabled, {@code false} otherwise.
     */
    public boolean isSSL() {
        return ssl;
    }

    /**
     * Checks if the webserver has a certain activation status in the builder.
     *
     * @param type The activation which should be present.
     * @return true if the activation status is equals, false otherwise.
     */
    public boolean isStatus(ServerState type) {
        return this.builder.isState(type);
    }

    /**
     * Returns the instance of {@link CraftsNet} bound to the server.
     *
     * @return The instance of {@link CraftsNet} bound to the server.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

    /**
     * Returns the logger provided by the associated CraftsNet instance.
     *
     * @return The {@link Logger} used by the owning CraftsNet instance.
     */
    public Logger getLogger() {
        return this.craftsNet.getLogger();
    }

    /**
     * Returns the builder associated with this server.
     *
     * @return The {@link B} instance containing this server's configuration.
     */
    public B getBuilder() {
        return builder;
    }

}
