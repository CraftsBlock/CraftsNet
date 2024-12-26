package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.builder.ActivateType;
import de.craftsblock.craftsnet.utils.SSL;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executors;

/**
 * The WebServer class represents a simple HTTP or HTTPS server that listens for incoming requests and handles
 * them based on registered API endpoints using the provided RouteRegistry.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.4.1
 * @see Exchange
 * @see RequestHandler
 * @see Route
 * @see WebHandler
 * @since 1.0.0-SNAPSHOT
 */
public class WebServer extends Server {

    private HttpServer server;

    /**
     * Constructs a WebServer with the specified port and SSL settings.
     *
     * @param craftsNet The CraftsNet instance which instantiates this webserver.
     * @param port      The port number to listen on.
     * @param ssl       A boolean flag indicating whether SSL encryption should be used (true for HTTPS, false for HTTP).
     */
    public WebServer(CraftsNet craftsNet, int port, boolean ssl) {
        super(craftsNet, port, ssl);
    }

    /**
     * {@inheritDoc}
     *
     * @param port    The port number to bind the server to.
     * @param backlog The maximum number of pending connections the server's socket may have in the queue.
     */
    @Override
    public void bind(int port, int backlog) {
        super.bind(port, backlog);
        if (logger != null) logger.info("Web server bound to port " + port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        if (running) return;

        logger.info("Starting web server on port " + port);
        try {
            // Create the HttpServer or HttpsServer based on the SSL flag.
            if (ssl) {
                // Load the SSL context using the provided SSL key files.
                SSLContext sslContext = SSL.load(this.craftsNet);
                if (sslContext != null) {
                    // Configure the HttpsServer with the SSL context.
                    HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
                    httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
                    server = httpsServer;
                }
            }
        } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException |
                 IOException e) {
            logger.error(e);
        } finally {
            if (server == null) {
                if (ssl)
                    logger.warning("SSl was not activated properly, using an http server as fallback!");

                try {
                    server = HttpServer.create(new InetSocketAddress(port), 0);
                } catch (IOException e) {
                    logger.error("Error while creating the " + (ssl ? "fallback" : "") + " http server.");
                    logger.error(e);
                }
            }
        }

        // Create a context for the root path ("/") and set its handler to process incoming requests.
        if (server == null) return;

        logger.debug("Creating the API handler");
        HttpContext context = server.createContext("/");
        context.setHandler(new WebHandler(this.craftsNet));

        logger.debug("Setting up the executor and starting the web server");
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        super.start();
        logger.debug("Web server has been started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!running) return;
        logger.debug("Web server will be stopped");
        server.stop(0);
        super.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void awakeOrWarn() {
        if (!isRunning() && isEnabled())
            // Start the web server as it is needed and currently not running
            this.craftsNet.webServer().start();
        else if (!isEnabled())
            // Print a warning if the web server is disabled and routes has been registered
            logger.warning("A route has been registered, but the web server is disabled!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sleepIfNotNeeded() {
        if (isRunning() && !craftsNet.routeRegistry().hasRoutes() && isStatus(ActivateType.DYNAMIC))
            stop();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return !isStatus(ActivateType.DISABLED);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isSSL() {
        return super.isSSL() && this.server instanceof HttpsServer;
    }

    /**
     * Checks if the websocket server has a certain activation status in the builder.
     *
     * @param type The activation which should be present.
     * @return true if the activation status is equals, false otherwise.
     */
    private boolean isStatus(ActivateType type) {
        return craftsNet.getBuilder().isWebServer(type);
    }

}
