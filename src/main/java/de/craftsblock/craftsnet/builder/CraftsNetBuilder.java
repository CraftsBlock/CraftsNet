package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.logging.EmptyLogger;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.logging.LoggerImpl;
import org.jetbrains.annotations.Range;

import java.io.IOException;

/**
 * Builder class for configuring the CraftsNet.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.3
 */
public class CraftsNetBuilder {

    private int webServerPort;
    private int webSocketServerPort;

    private ActivateType webServer;
    private ActivateType webSocketServer;
    private ActivateType addonSystem;
    private ActivateType commandSystem;

    private ActivateType fileLogger;
    private Logger logger;

    private boolean debug;
    private boolean tempFilesOnNormalFileSystem;
    private boolean skipVersionCheck;
    private boolean ssl;

    private long logRotate;

    /**
     * Constructs a new Builder instance with default configuration settings.
     */
    public CraftsNetBuilder() {
        webServerPort = 5000;
        webSocketServerPort = 5001;
        webServer = webSocketServer = ActivateType.DYNAMIC;
        addonSystem = commandSystem = fileLogger = ActivateType.ENABLED;
        withDebug(false);
        withTempFilesOnNormalFileSystem(false);
        withSkipVersionCheck(false);
        withSSL(false);
        withoutLogRotate();
    }

    /**
     * Specifies the port for the web server.
     *
     * @param port The port number for the web server.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withWebServer(int port) {
        return withWebServer(ActivateType.DYNAMIC, port);
    }

    /**
     * Specifies the port for the web server.
     *
     * @param type The activation type for the web server.
     * @return The Builder instance.
     * @since 3.0.5
     */
    public CraftsNetBuilder withWebServer(ActivateType type) {
        return withWebServer(type, this.webServerPort);
    }

    /**
     * Specifies the activation type and port for the web server.
     *
     * @param type The activation type for the web server.
     * @param port The port number for the web server.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withWebServer(ActivateType type, int port) {
        this.webServer = type;
        this.webServerPort = port;
        return this;
    }

    /**
     * Specifies the port for the WebSocket server.
     *
     * @param port The port number for the WebSocket server.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withWebSocketServer(int port) {
        return withWebSocketServer(ActivateType.DYNAMIC, port);
    }

    /**
     * Specifies the activation type and port for the WebSocket server.
     *
     * @param type The activation type for the WebSocket server.
     * @return The Builder instance.
     * @since 3.0.5
     */
    public CraftsNetBuilder withWebSocketServer(ActivateType type) {
        return withWebSocketServer(type, this.webSocketServerPort);
    }

    /**
     * Specifies the activation type and port for the WebSocket server.
     *
     * @param type The activation type for the WebSocket server.
     * @param port The port number for the WebSocket server.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withWebSocketServer(ActivateType type, int port) {
        this.webSocketServer = type;
        this.webSocketServerPort = port;
        return this;
    }

    /**
     * Specifies the activation type for the addon system.
     *
     * @param type The activation type for the addon system.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withAddonSystem(ActivateType type) {
        this.addonSystem = type;
        return this;
    }

    /**
     * Specifies the activation type for the command system.
     *
     * @param type The activation type for the command system.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withCommandSystem(ActivateType type) {
        this.commandSystem = type;
        return this;
    }

    /**
     * Specifies the activation type for the file logger.
     *
     * @param type The activation type for the file logger.
     * @return The Builder instance.
     * @since 3.0.5
     */
    public CraftsNetBuilder withFileLogger(ActivateType type) {
        this.fileLogger = type;
        return this;
    }

    /**
     * Configures whether temporary files should be placed on the normal file system.
     * When set to {@code true}, the application will store temporary files in the local
     * file system instead of using the system's default temporary file location.
     *
     * @param tempFilesOnNormalFileSystem {@code true} to place temporary files in the normal file system, {@code false} otherwise.
     * @return the current {@code Builder} instance for method chaining.
     */
    public CraftsNetBuilder withTempFilesOnNormalFileSystem(boolean tempFilesOnNormalFileSystem) {
        this.tempFilesOnNormalFileSystem = tempFilesOnNormalFileSystem;
        return this;
    }

    /**
     * Specifies whether the version check should be skipped on startup.
     *
     * @param skipVersionCheck true if the version check should be skipped, false otherwise.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withSkipVersionCheck(boolean skipVersionCheck) {
        this.skipVersionCheck = skipVersionCheck;
        return this;
    }

    /**
     * Sets a custom logger which will be used by CraftsNet. It can be null when the default logger should be used.
     *
     * @param logger The instance of the custom logger.
     * @return The Builder instance.
     * @since 3.0.5
     */
    public CraftsNetBuilder withCustomLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Specifies the activation type for the logger.
     *
     * @param type The activation type for the logger.
     * @return The Builder instance.
     * @since 3.0.5
     */
    public CraftsNetBuilder withLogger(ActivateType type) {
        if (type.equals(ActivateType.DISABLED))
            this.logger = this.logger instanceof EmptyLogger ? this.logger : new EmptyLogger(this.logger);
        else if (this.logger instanceof EmptyLogger logger)
            this.logger = logger.previous();
        return this;
    }

    /**
     * Specifies whether debug mode should be enabled.
     *
     * @param enabled true if debug mode should be enabled, false otherwise.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withDebug(boolean enabled) {
        this.debug = enabled;
        return this;
    }

    /**
     * Specifies whether SSL should be enabled.
     *
     * @param enabled true if SSL should be enabled, false otherwise.
     * @return The Builder instance.
     */
    public CraftsNetBuilder withSSL(boolean enabled) {
        ssl = enabled;
        return this;
    }

    /**
     * Sets the log rotation size threshold for this builder.
     *
     * @param logRotate the log rotation size threshold, in bytes. A value of 0 disables log rotation.
     *                  Must be non-negative.
     * @return this {@code Builder} instance, enabling method chaining.
     */
    public CraftsNetBuilder withLogRotate(@Range(from = 0, to = Long.MAX_VALUE) long logRotate) {
        this.logRotate = logRotate;
        return this;
    }

    /**
     * Disables log rotation by setting the log rotation size threshold to 0.
     *
     * @return this {@code Builder} instance, enabling method chaining.
     */
    public CraftsNetBuilder withoutLogRotate() {
        return withLogRotate(0);
    }

    /**
     * Retrieves the port number configured for the web server.
     *
     * @return The port number for the web server.
     */
    public int getWebServerPort() {
        return webServerPort;
    }

    /**
     * Retrieves the activation type configured for the web server.
     *
     * @return The activation type for the web server.
     */
    public ActivateType getWebServer() {
        return webServer;
    }

    /**
     * Checks if the web server is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the web server is configured with the specified activation type, false otherwise.
     */
    public boolean isWebServer(ActivateType type) {
        return webServer == type;
    }

    /**
     * Retrieves the port number configured for the WebSocket server.
     *
     * @return The port number for the WebSocket server.
     */
    public int getWebSocketServerPort() {
        return webSocketServerPort;
    }

    /**
     * Retrieves the activation type configured for the WebSocket server.
     *
     * @return The activation type for the WebSocket server.
     */
    public ActivateType getWebSocketServer() {
        return webSocketServer;
    }

    /**
     * Checks if the WebSocket server is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the WebSocket server is configured with the specified activation type, false otherwise.
     */
    public boolean isWebSocketServer(ActivateType type) {
        return webSocketServer == type;
    }

    /**
     * Retrieves the activation type configured for the addon system.
     *
     * @return The activation type for the addon system.
     */
    public ActivateType getAddonSystem() {
        return addonSystem;
    }

    /**
     * Checks if the addon system is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the addon system is configured with the specified activation type, false otherwise.
     */
    public boolean isAddonSystem(ActivateType type) {
        return addonSystem == type;
    }

    /**
     * Retrieves the activation type configured for the command system.
     *
     * @return The activation type for the command system.
     */
    public ActivateType getCommandSystem() {
        return commandSystem;
    }

    /**
     * Checks if the command system is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the command system is configured with the specified activation type, false otherwise.
     */
    public boolean isCommandSystem(ActivateType type) {
        return commandSystem == type;
    }

    /**
     * Checks if the file logger is configured with the specified activation type.
     *
     * @param type The activation type to check.
     * @return true if the file logger is configured with the specified activation type, false otherwise.
     * @since 3.0.5
     */
    public boolean isFileLogger(ActivateType type) {
        return fileLogger == type;
    }

    /**
     * Determines whether temporary files should be placed in the normal file system.
     *
     * @return {@code true} if temporary files are placed on the normal file system, {@code false} otherwise.
     */
    public boolean shouldPlaceTempFilesOnNormalFileSystem() {
        return tempFilesOnNormalFileSystem;
    }

    /**
     * Determines whether the version check should be skipped on startup.
     *
     * @return {@code true} if the version check should be skipped, {@code false} otherwise.
     */
    public boolean shouldSkipVersionCheck() {
        return skipVersionCheck;
    }

    /**
     * Retrieves the custom logger which should be used by CraftsNet.
     *
     * @return The logger instance.
     * @since 3.0.5
     */
    public Logger getCustomLogger() {
        return logger;
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is enabled, false otherwise.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Checks if SSL is enabled.
     *
     * @return true if SSL is enabled, false otherwise.
     */
    public boolean isSSL() {
        return ssl;
    }

    /**
     * Retrieves the log rotation size threshold.
     *
     * @return the log rotation size threshold in bytes, or 0 if log rotation is disabled.
     */
    public long getLogRotate() {
        return logRotate;
    }

    /**
     * Checks if log rotation is disabled.
     *
     * @return {@code true} if log rotation is disabled (i.e., the threshold is 0), {@code false} otherwise.
     */
    public boolean isLogRotate() {
        return logRotate == 0;
    }

    /**
     * Builds and starts the CraftsNet framework with the configured settings.
     *
     * @return The constructed CraftsNet instance.
     * @throws IOException If an I/O error occurs during the startup process.
     */
    public CraftsNet build() throws IOException {
        // Set up the logger if no logger was given
        if (logger == null) logger = new LoggerImpl(isDebug());

        CraftsNet craftsNet = new CraftsNet();
        craftsNet.start(this);
        return craftsNet;
    }

}
