package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.*;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.events.RequestEvent;
import de.craftsblock.craftsnet.events.shares.ShareFileLoadedEvent;
import de.craftsblock.craftsnet.events.shares.ShareRequestEvent;
import de.craftsblock.craftsnet.utils.Logger;
import de.craftsblock.craftsnet.utils.SSL;
import org.apache.tika.Tika;

import javax.net.ssl.SSLContext;
import java.io.File;
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
 * @version 1.4
 * @see Exchange
 * @see RequestHandler
 * @see Route
 * @since 1.0.0
 */
public class WebServer {

    private final HttpServer server;
    private final Logger logger;
    private final Tika tika = new Tika();

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
        logger.debug("Web server jvm shutdown hook is integrated");
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // Create the HttpServer or HttpsServer based on the SSL flag.
        logger.info("Web server will be started on port " + port);
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
        logger.debug("Creating the API handler");
        server.createContext("/").setHandler(exchange -> {
            RouteRegistry registry = CraftsNet.routeRegistry;
            try (exchange; Response response = new Response(exchange)) {
                // Extract relevant information from the incoming request.
                String domain = exchange.getRequestHeaders().getFirst("Host").split(":")[0];
                String requestMethod = exchange.getRequestMethod();
                String url = exchange.getRequestURI().toString();
                String[] stripped = url.split("\\?");
                String query = (stripped.length == 2 ? stripped[1] : "");
                String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
                Headers headers = exchange.getRequestHeaders();
                if (headers.containsKey("X-forwarded-for"))
                    ip = headers.getFirst("X-forwarded-for").split(", ")[0];
                if (headers.containsKey("Cf-connecting-ip"))
                    ip = headers.getFirst("Cf-connecting-ip");

                if (registry.isShare(url) && HttpMethod.parse(requestMethod) == HttpMethod.GET) {
                    File folder = registry.getShareFolder(url);
                    Matcher matcher = registry.getSharePattern(url).matcher(url);
                    if (!matcher.matches()) {
                        Json config = JsonParser.parse("{}");
                        config.set("error", "There was an unexpected error while matching!");
                        response.print(config.asString());
                        return;
                    }

                    String path = matcher.group(1);
                    ShareRequestEvent event = new ShareRequestEvent(path);
                    CraftsNet.listenerRegistry.call(event);
                    for (String key : event.getHeaders().keySet())
                        response.setHeader(key, event.getHeaders().getFirst(key));
                    if (event.isCancelled()) {
                        logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[SHARE ABORTED]");
                        return;
                    }
                    path = event.getPath();
                    logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;205m[SHARED]");

                    File share = new File(folder, (path.isBlank() ? "index.html" : path));
                    ShareFileLoadedEvent fileLoadedEvent = new ShareFileLoadedEvent(share, tika);
                    CraftsNet.listenerRegistry.call(fileLoadedEvent);
                    share = fileLoadedEvent.getFile();

                    if (!share.getCanonicalPath().startsWith(folder.getCanonicalPath() + File.separator)) {
                        response.setCode(403);
                        response.setContentType("text/html; charset=utf-8");
                        response.print(DefaultPages.notallowed(domain, exchange.getLocalAddress().getPort()));
                        return;
                    } else if (!share.exists()) {
                        response.setCode(404);
                        response.setContentType("text/html; charset=utf-8");
                        response.print(DefaultPages.notfound(domain, exchange.getLocalAddress().getPort()));
                        return;
                    }
                    response.setContentType(fileLoadedEvent.getContentType());
                    response.print(share);
                    return;
                }

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

                // Find the registered route mapping based on the URL and request method.
                RouteRegistry.RouteMapping route = registry.getRoute(url, domain, headers.keySet(), requestMethod);

                // If no matching route is found, return an error response.
                if (route == null) {
                    Json config = JsonParser.parse("{}");
                    config.set("error", "Path do not match any API endpoint!");
                    response.print(config.asString());
                    logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[NOT FOUND]");
                    return;
                }
                // Associate the matched route with the Request object.
                request.setRoute(route);

                // Create a RequestEvent and call listeners before invoking the API handler method.
                RequestEvent event = new RequestEvent(new Exchange(url, request, response), route);
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
                args[0] = new Exchange(url, request, response);
                for (int i = 2; i <= matcher.groupCount(); i++)
                    args[i - 1] = matcher.group(i);

                // Invoke the API handler method with the extracted path parameters.
                route.method().invoke(route.handler(), args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error(e);
            }
        });

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
