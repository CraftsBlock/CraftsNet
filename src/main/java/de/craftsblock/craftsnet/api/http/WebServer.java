package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.*;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.events.RequestEvent;
import de.craftsblock.craftsnet.utils.Logger;
import de.craftsblock.craftsnet.utils.SSL;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

/**
 * The WebServer class represents a simple HTTP or HTTPS server that listens for incoming requests and handles
 * them based on registered API endpoints using the provided RouteRegistry.
 *
 * @author CraftsBlock
 * @version 1.3
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
     * @param port    The port number to listen on.
     * @param ssl     A boolean flag indicating whether SSL encryption should be used (true for HTTPS, false for HTTP).
     * @param ssl_key The key which is used to secure the private key while running (applicable only when ssl is true).
     * @throws IOException If an I/O error occurs while creating the server.
     */
    public WebServer(int port, boolean ssl, String ssl_key) throws IOException {
        logger = CraftsNet.logger;

        // Add a JVM shutdown hook to gracefully stop the server when the JVM exits.
        logger.debug("Web Server JVM Shutdown Hook wird eingebunden");
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // Create the HttpServer or HttpsServer based on the SSL flag.
        logger.info("Web Server wird auf Port " + port + " gestartet");
        if (!ssl) server = HttpServer.create(new InetSocketAddress(port), 0);
        else {
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            try {
                // Load the SSL context using the provided SSL key files.
                SSLContext sslContext = SSL.load("./certificates/fullchain.pem", "./certificates/privkey.pem", ssl_key);
                // Configure the HttpsServer with the SSL context.
                ((HttpsServer) server).setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    @Override
                    public void configure(HttpsParameters params) {
                        params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
                    }
                });
            } catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException |
                     CertificateException e) {
                e.printStackTrace();
            }
        }

        // Create a context for the root path ("/") and set its handler to process incoming requests.
        logger.debug("Erstellen des API Handlers");
        server.createContext("/").setHandler(exchange -> {
            try (exchange; Response response = new Response(exchange)) {
                // Extract relevant information from the incoming request.
                String domain = exchange.getRequestHeaders().getFirst("Host");
                String url = exchange.getRequestURI().toString();
                String[] stripped = url.split("\\?");
                String query = (stripped.length == 2 ? stripped[1] : "");
                String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
                Headers headers = exchange.getRequestHeaders();
                if (headers.containsKey("X-forwarded-for"))
                    ip = headers.getFirst("X-forwarded-for").split(", ")[0];
                if (headers.containsKey("Cf-connecting-ip"))
                    ip = headers.getFirst("Cf-connecting-ip");

                // Create a Request object to encapsulate the incoming request information.
                Request request = new Request(exchange, query, ip);
                url = stripped[0];

                // Check if the requested URL contains "/favicon.ico". If so, return an error response.
                if (url.contains("favicon.ico")) {
                    Json json = JsonParser.parse("{}");
                    json.set("success", false);
                    json.set("message", "No permission!");
                    response.print(json.asString());
                    return;
                }

                String requestMethod = exchange.getRequestMethod(); // Get the HTTP request method (e.g., GET, POST).
                RouteRegistry.RouteMapping route = CraftsNet.routeRegistry.getRoute(url, domain, requestMethod); // Find the registered route mapping based on the URL and request method.

                // If no matching route is found, return an error response.
                if (route == null) {
                    Json config = JsonParser.parse("{}");
                    config.set("error", "Path do not match any API endpoint!");
                    response.print(config.asString());
                    logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[NOT FOUND]");
                    return;
                }
                request.setRoute(route); // Associate the matched route with the Request object.

                // Create a RequestEvent and call listeners before invoking the API handler method.
                RequestEvent event = new RequestEvent(new Exchange(request, response), route);
                CraftsNet.listenerRegistry.call(event);
                if (event.isCancelled()) {
                    logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[ABORTED]");
                    return;
                }
                logger.info(requestMethod + " " + url + " from " + ip);

                // Use a regular expression matcher to extract path parameters from the URL.
                Matcher matcher = route.validator().matcher(url);
                if (!matcher.matches()) {
                    Json config = JsonParser.parse("{}");
                    config.set("error", "There was an unexpected error while matching!");
                    response.print(config.asString());
                    return;
                }

                // Prepare the argument array to be passed to the API handler method.
                Object[] args = new Object[matcher.groupCount()];
                args[0] = new Exchange(request, response);
                for (int i = 2; i <= matcher.groupCount(); i++)
                    args[i - 1] = matcher.group(i);

                route.method().invoke(route.handler(), args); // Invoke the API handler method with the extracted path parameters.
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error(e);
            }
        });

        logger.debug("Web Server wird gestartet");
        server.setExecutor(Executors.newFixedThreadPool(15));
        server.start();
    }

    /**
     * Stops the web server.
     */
    public void stop() {
        logger.debug("Web Server wird gestoppt");
        server.stop(0);
    }

}
