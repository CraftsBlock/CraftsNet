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
import de.craftsblock.craftsnet.builder.server.WebServerBuilder;

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
public class WebServer extends Server<WebServerBuilder> {

    private Executor executor;

    private HttpServer server;

    /**
     * Constructs a {@link WebServer} with the specified port and SSL settings.
     *
     * @param craftsNet The CraftsNet instance which instantiates this webserver.
     * @param builder   The {@link WebServerBuilder} which should build this web server.
     */
    public WebServer(CraftsNet craftsNet, WebServerBuilder builder) {
        super(craftsNet, builder);
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
        this.getLogger().info("Web server bound to port %s", port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        WebServerBuilder builder = this.getBuilder();
        System.setProperty("sun.net.httpserver.maxReqHeaders", String.valueOf(builder.maxHeaders()));
        System.setProperty("sun.net.httpserver.maxReqHeaderSize", String.valueOf(builder.maxHeaderSize()));

        getLogger().info("Starting web server on port %s", port);
        try {
            // Create the HttpServer or HttpsServer based on the SSL flag.
            if (ssl) {
                SSLContext sslContext = SSL.load(this.getCraftsNet());
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

        this.getLogger().debug("Creating the API handler");
        HttpContext context = server.createContext("/");
        context.setHandler(new WebHandler(this.getCraftsNet(), this));

        this.getLogger().debug("Setting up the executor and starting the web server");
        executor = Objects.requireNonNull(
                this.getExecutorFactory().apply(builder),
                "Executor factory returned null"
        );
        server.setExecutor(executor);

        server.start();
        super.start();
        this.getLogger().debug("Web server has been started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }

        this.getLogger().debug("Web server will be stopped");

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
        if (!isRunning() && isEnabled()) {
            // Start the web server as it is needed and currently not running
            this.getCraftsNet().getWebServer().start();
        } else if (!isEnabled()) {
            // Print a warning if the web server is disabled and routes has been registered
            this.getLogger().warning("A route has been registered, but the web server is disabled!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sleepIfNotNeeded() {
        if (isRunning() && !this.getCraftsNet().getRouteRegistry().hasRoutes()
                && this.isStatus(ServerState.DYNAMIC)) {
            stop();
        }
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

}
