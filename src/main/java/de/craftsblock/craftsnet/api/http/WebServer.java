package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.utils.Logger;
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
 * @version 1.4
 * @see Exchange
 * @see RequestHandler
 * @see Route
 * @since 1.0.0
 */
public class WebServer {

    private final HttpServer server;
    private final Logger logger;

    /**
     * Constructs a WebServer with the specified port and SSL settings.
     *
     * @param port The port number to listen on.
     * @param ssl  A boolean flag indicating whether SSL encryption should be used (true for HTTPS, false for HTTP).
     * @throws IOException If an I/O error occurs while creating the server.
     */
    public WebServer(int port, boolean ssl) throws IOException {
        logger = CraftsNet.logger();

        // Add a JVM shutdown hook to gracefully stop the server when the JVM exits.
        logger.debug("Web server jvm shutdown hook is integrated");
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // Create the HttpServer or HttpsServer based on the SSL flag.
        logger.info("Web server will be started on port " + port);
        if (!ssl) server = HttpServer.create(new InetSocketAddress(port), 0);
        else {
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            try {
                // Load the SSL context using the provided SSL key files.
                SSLContext sslContext = SSL.load("./certificates/fullchain.pem", "./certificates/privkey.pem");
                // Configure the HttpsServer with the SSL context.
                ((HttpsServer) server).setHttpsConfigurator(new HttpsConfigurator(sslContext));
            } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException |
                     CertificateException e) {
                e.printStackTrace();
            }
        }

        // Create a context for the root path ("/") and set its handler to process incoming requests.
        logger.debug("Creating the API handler");
        server.createContext("/").setHandler(new WebHandler());

        logger.debug("Setting up the executor and starting the web server");
        server.setExecutor(Executors.newFixedThreadPool(25));
        server.start();
        logger.debug("Web server has been started");
    }

    /**
     * Stops the web server.
     */
    public void stop() {
        logger.debug("Web server will be stopped");
        server.stop(0);
    }

}
