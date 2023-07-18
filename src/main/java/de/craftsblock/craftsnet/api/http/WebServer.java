package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.*;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.Main;
import de.craftsblock.craftsnet.api.RouteRegistry;
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
import java.util.regex.Matcher;

public class WebServer {

    private final HttpServer server;
    private final Logger logger;

    public WebServer(int port, boolean ssl, String ssl_key) throws IOException {
        logger = Main.logger;
        logger.debug("Web Server JVM Shutdown Hook wird eingebunden");
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        logger.info("Web Server wird auf Port " + port + " gestartet");
        if (!ssl) server = HttpServer.create(new InetSocketAddress(port), 0);
        else {
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            try {
                SSLContext sslContext = SSL.load("./certificates/fullchain.pem", "./certificates/privkey.pem", ssl_key);
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


        logger.debug("Erstellen des API Handlers");
        server.createContext("/").setHandler(exchange -> {
            try (exchange; Response response = new Response(exchange)) {
                String url = exchange.getRequestURI().toString();
                String[] stripped = url.split("\\?");
                String query = (stripped.length == 2 ? stripped[1] : "");
                String ip = exchange.getRemoteAddress().getAddress().getHostAddress();

                Headers headers = exchange.getRequestHeaders();
                if (headers.containsKey("X-forwarded-for"))
                    ip = headers.getFirst("X-forwarded-for").split(", ")[0];
                if (headers.containsKey("Cf-connecting-ip"))
                    ip = headers.getFirst("Cf-connecting-ip");

                Request request = new Request(exchange, query, ip);

                url = stripped[0];
                if (url.contains("favicon.ico")) {
                    Json json = JsonParser.parse("{}");
                    json.set("success", false);
                    json.set("message", "No permission!");
                    response.print(json.asString());
                    return;
                }

                String requestMethod = exchange.getRequestMethod();
                RouteRegistry.RouteMapping route = Main.routeRegistry.getRoute(url, requestMethod);

                RequestEvent event = new RequestEvent(new Exchange(request, response), route);
                Main.listenerRegistry.call(event);
                if (event.isCancelled()) {
                    logger.info(requestMethod + " " + url + " from " + ip + " \u001b[38;5;9m[ABORTED]");
                    return;
                }
                logger.info(requestMethod + " " + url + " from " + ip);

                if (route == null) {
                    Json config = JsonParser.parse("{}");
                    config.set("error", "Path do not match any API endpoint!");
                    response.print(config.asString());
                    return;
                }
                request.setRoute(route);

                Matcher matcher = route.validator().matcher(url);
                if (!matcher.matches()) {
                    Json config = JsonParser.parse("{}");
                    config.set("error", "There was an unexpected error while matching!");
                    response.print(config.asString());
                    return;
                }

                Object[] args = new Object[matcher.groupCount()];
                args[0] = new Exchange(request, response);
                for (int i = 2; i <= matcher.groupCount(); i++)
                    args[i - 1] = matcher.group(i);
                route.method().invoke(route.handler(), args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        logger.debug("Web Server wird gestartet");
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        logger.debug("Web Server wird gestoppt");
        server.stop(0);
    }

}
