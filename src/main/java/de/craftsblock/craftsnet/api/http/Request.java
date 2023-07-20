package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftscore.utils.Validator;
import de.craftsblock.craftsnet.api.RouteRegistry;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * The Request class represents an incoming HTTP request received by the web server.
 * It encapsulates information related to the request, such as headers, query parameters, cookies, and request body.
 * <p>
 * The class is responsible for parsing and providing access to various elements of the request, making it easier for
 * request handlers to process and respond to the incoming requests.
 *
 * @author CraftsBlock
 * @see Exchange
 * @since 1.0.0
 */
public class Request {

    private final HttpExchange exchange;
    private final Headers headers;
    private final Json queryParams = JsonParser.parse("{}");
    private final Json cookies = JsonParser.parse("{}");
    private final String ip;

    private String body;
    private RouteRegistry.RouteMapping route;

    /**
     * Constructs a new Request object.
     *
     * @param exchange The HttpExchange object representing the incoming HTTP request.
     * @param query    The query string extracted from the request URI.
     * @param ip       The IP address of the client sending the request.
     */
    public Request(HttpExchange exchange, String query, String ip) {
        this.exchange = exchange;
        this.headers = exchange.getRequestHeaders();
        this.ip = ip;

        Arrays.stream(query.split("&")).forEach(pair -> {
            String[] stripped = pair.split("=");
            if (stripped.length != 2)
                return;
            queryParams.set(stripped[0], stripped[1]);
        });

        if (headers.containsKey("cookie"))
            Arrays.stream(headers.getFirst("cookie").split("; ")).forEach(pair -> {
                String[] stripped = pair.split("=");
                if (stripped.length != 2)
                    return;
                cookies.set(stripped[0], stripped[1]);
            });
    }

    /**
     * Checks if the request contains the specified query parameter.
     *
     * @param key The key of the query parameter to check.
     * @return True if the request contains the specified query parameter, false otherwise.
     */
    public boolean hasParam(String key) {
        return queryParams.contains(key);
    }

    /**
     * Retrieves the value of the specified query parameter from the request.
     *
     * @param key The key of the query parameter to retrieve.
     * @return The value of the specified query parameter, or null if the parameter is not found.
     */
    @Nullable
    public String retrieveParam(String key) {
        if (hasParam(key))
            return queryParams.getString(key);
        return null;
    }

    /**
     * Checks if the request contains the specified cookie.
     *
     * @param key The name of the cookie to check.
     * @return True if the request contains the specified cookie, false otherwise.
     */
    public boolean hasCookie(String key) {
        return cookies.contains(key);
    }

    /**
     * Retrieves the value of the specified cookie from the request.
     *
     * @param key The name of the cookie to retrieve.
     * @return The value of the specified cookie, or null if the cookie is not found.
     */
    @Nullable
    public String retrieveCookie(String key) {
        if (hasCookie(key))
            return cookies.getString(key);
        return null;
    }

    /**
     * Retrieves the matched route mapping for the request.
     *
     * @return The RouteMapping object representing the matched route, or null if no route is matched.
     */
    @Nullable
    public RouteRegistry.RouteMapping getRoute() {
        return route;
    }

    /**
     * Checks if the request has a valid JSON-formatted body.
     *
     * @return True if the request has a valid JSON body, false otherwise.
     */
    public boolean hasBody() {
        return getBody() != null;
    }

    /**
     * Retrieves the JSON object from the request body.
     *
     * @return The JSON object parsed from the request body, or null if the request body is not a valid JSON.
     */
    @Nullable
    public Json getBody() {
        if (body != null)
            return JsonParser.parse(body);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            body = reader.readLine();
            if (Validator.isJsonValid(body)) return JsonParser.parse(body);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Checks if the request contains the specified header.
     *
     * @param name The name of the header to check.
     * @return True if the request contains the specified header, false otherwise.
     */
    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    /**
     * Retrieves the value of the specified header from the request.
     *
     * @param name The name of the header to retrieve.
     * @return The value of the specified header, or null if the header is not found.
     */
    @Nullable
    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    /**
     * Retrieves the IP address of the client sending the request.
     *
     * @return The IP address of the client.
     */
    public String getIp() {
        return ip;
    }

    /**
     * Retrieves the RequestMethod of the request (e.g., GET, POST, PUT, etc.).
     *
     * @return The RequestMethod enum representing the request method.
     */
    public RequestMethod getRequestMethod() {
        return RequestMethod.parse(exchange.getRequestMethod());
    }

    /**
     * Retrieves the HttpExchange object representing the incoming HTTP request.
     *
     * @return The HttpExchange object.
     */
    public HttpExchange unsafe() {
        return exchange;
    }

    /**
     * Sets the matched route mapping for the request.
     *
     * @param route The RouteMapping object representing the matched route.
     */
    protected void setRoute(RouteRegistry.RouteMapping route) {
        this.route = route;
    }

}
