package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.api.ssl.SSL;
import de.craftsblock.craftsnet.builder.server.ServerState;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The WebServer class represents a simple HTTP or HTTPS server that listens for incoming requests and handles
 * them based on registered API endpoints using the provided RouteRegistry.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see Exchange
 * @see RequestHandler
 * @see Route
 * @see WebHandler
 * @since 1.0.0-SNAPSHOT
 */
public class WebServer extends Server {

    private Supplier<Executor> executorFactory = Executors::newVirtualThreadPerTaskExecutor;
    private Executor executor;

    private HttpServer server;

    /**
     * Constructs a {@link WebServer} with the specified port and SSL settings.
     *
     * @param craftsNet The CraftsNet instance which instantiates this webserver.
     * @param port      The port number to listen on.
     * @param ssl       A boolean flag indicating whether SSL encryption should be used (true for HTTPS, false for HTTP).
     */
    public WebServer(CraftsNet craftsNet, int port, boolean ssl) {
        this(craftsNet, port, 0, ssl);
    }

    /**
     * Constructs a {@link WebServer} instance with the specified port number and backlog size.
     *
     * @param craftsNet The CraftsNet instance which instantiates this webserver.
     * @param port      The port number on which the server will listen for incoming connections.
     * @param backlog   The size of the backlog for the webserver.
     * @param ssl       A boolean flag indicating whether SSL encryption should be used.
     */
    public WebServer(CraftsNet craftsNet, int port, int backlog, boolean ssl) {
        super(craftsNet, port, backlog, ssl);
    }

    /**
     * {@inheritDoc}
     *
     * @param port    The port number to bind the server to.
     * @param backlog The maximum number of pending connections the server's socket may have in the queue.
     */
    @Override
    public synchronized void bind(int port, int backlog) {
        super.bind(port, backlog);

        if (logger != null) {
            logger.info("Web server bound to port %s", port);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        logger.info("Starting web server on port %s", port);
        try {
            // Create the HttpServer or HttpsServer based on the SSL flag.
            if (ssl) {
                SSLContext sslContext = SSL.load(this.craftsNet);
                if (sslContext == null) {
                    throw new IllegalStateException("Failed to initialize SSL context");
                }

                HttpsServer httpsServer = HttpsServer.create(
                        new InetSocketAddress(port),
                        backlog
                );

                httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
                server = httpsServer;
            } else {
                server = HttpServer.create(new InetSocketAddress(port), backlog);
            }
        } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException |
                 NoSuchAlgorithmException | CertificateException | IOException |
                 InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to initialize SSL context");
        }

        logger.debug("Creating the API handler");
        HttpContext context = server.createContext("/");
        context.setHandler(new WebHandler(this.craftsNet));

        logger.debug("Setting up the executor and starting the web server");
        executor = Objects.requireNonNull(
                executorFactory.get(),
                "Executor factory returned null"
        );
        server.setExecutor(executor);

        server.start();
        super.start();
        logger.debug("Web server has been started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }

        logger.debug("Web server will be stopped");

        try {
            server.stop(0);

            if (executor instanceof AutoCloseable closeable) {
                closeable.close();
            }
            executor = null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop the webserver properly", e);
        }

        server = null;
        super.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void awakeOrWarn() {
        if (!isRunning() && isEnabled())
            // Start the web server as it is needed and currently not running
            this.craftsNet.getWebServer().start();
        else if (!isEnabled())
            // Print a warning if the web server is disabled and routes has been registered
            logger.warning("A route has been registered, but the web server is disabled!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void sleepIfNotNeeded() {
        if (isRunning() && !craftsNet.getRouteRegistry().hasRoutes() && isStatus(ServerState.DYNAMIC))
            stop();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return !isStatus(ServerState.DISABLED);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isSSL() {
        return this.server instanceof HttpsServer;
    }

    /**
     * Checks if SSL should be enabled for the server.
     *
     * @return {@code true} if SSL should be enabled, {@code false} otherwise.
     */
    public boolean shouldUseSSL() {
        return super.isSSL();
    }

    /**
     * Checks if the webserver has a certain activation status in the builder.
     *
     * @param type The activation which should be present.
     * @return true if the activation status is equals, false otherwise.
     */
    private boolean isStatus(ServerState type) {
        return craftsNet.getBuilder().webServer().isState(type);
    }

    /**
     * Sets the factory used to create the executor responsible for handling HTTP requests.
     *
     * @param executorFactory The factory used to create the executor.
     */
    public void setExecutorFactory(@NotNull Supplier<Executor> executorFactory) {
        Objects.requireNonNull(executorFactory, "executorFactory");
        this.executorFactory = executorFactory;
    }

    /**
     * Retrieves the factory used to create the executor responsible for handling HTTP requests.
     *
     * @return The factory used to create the executor.
     */
    public @NotNull Supplier<Executor> getExecutorFactory() {
        return executorFactory;
    }

}
