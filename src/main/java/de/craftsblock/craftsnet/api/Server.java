package de.craftsblock.craftsnet.api;

/**
 * Abstract class representing a server in the CraftsNet framework.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-3.0.3
 */
public abstract class Server {

    protected int port;
    protected int backlog;
    protected boolean ssl;

    protected boolean running;

    /**
     * Constructs a server with the specified port and SSL configuration.
     *
     * @param port The port number for the server.
     * @param ssl  true if SSL should be used, false otherwise.
     */
    public Server(int port, boolean ssl) {
        this(port, 0, ssl);
    }

    /**
     * Constructs a server with the specified port, backlog, and SSL configuration.
     *
     * @param port    The port number for the server.
     * @param backlog The maximum number of pending connections the server's socket may have in the queue.
     * @param ssl     true if SSL should be used, false otherwise.
     */
    public Server(int port, int backlog, boolean ssl) {
        this.bind(port, backlog);
        this.ssl = ssl;
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
     * @param ssl true if SSL should be used, false otherwise.
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
        return true;
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
     * @return true if SSL is enabled, false otherwise.
     */
    public boolean isSSL() {
        return ssl;
    }

}
