package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.body.Body;
import de.craftsblock.craftsnet.api.http.body.JsonBody;
import de.craftsblock.craftsnet.api.http.body.MultipartFormBody;
import de.craftsblock.craftsnet.api.http.body.StandardFormBody;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * The Request class represents an incoming HTTP request received by the web server.
 * It encapsulates information related to the request, such as headers, query parameters, cookies, and request body.
 * <p>
 * The class is responsible for parsing and providing access to various elements of the request, making it easier for
 * request handlers to process and respond to the incoming requests.
 *
 * @author CraftsBlock
 * @version 1.4
 * @see Exchange
 * @since 1.0.0
 */
public class Request implements AutoCloseable {

    private final HttpExchange exchange;
    private final Headers headers;
    private final Json queryParams = JsonParser.parse("{}");
    private final Json cookies = JsonParser.parse("{}");
    private final String ip;

    private Body body;
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
     * Closes the Request object, releasing associated resources such as the HttpExchange and request headers.
     * If a request body is present, it is also closed to free any related resources.
     *
     * @throws Exception If an error occurs while closing the Request object or its associated resources.
     */
    @Override
    public void close() throws Exception {
        exchange.close();
        headers.clear();
        if (body != null) body.close();
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
     * Checks if the HTTP request has a request body.
     *
     * @return {@code true} if a request body exists, otherwise {@code false}.
     */
    public boolean hasBody() {
        return getBody() != null;
    }

    /**
     * Retrieves the HTTP request body, if it exists.
     *
     * @return The HTTP request body as a {@code Body} object, or {@code null} if no body exists or an error occurs.
     */
    @Nullable
    public Body getBody() {
        // Check if the body has already been obtained
        if (body != null) return body;
        try {
            // Check the Content-Type header to determine the type of request body
            if (headers.getFirst("Content-Type").startsWith("multipart/form-data")) {
                // If it's a multipart/form-data request, extract the boundary
                String[] boundary = headers.getFirst("Content-Type").split("=");
                if (boundary.length != 2) return null; // Ensure that the Content-Type header contains a valid boundary
                body = new MultipartFormBody(boundary[1], exchange.getRequestBody()); // Create a new MultipartFormBody using the boundary and the request body
            } else if (headers.getFirst("Content-Type").startsWith("application/x-www-form-urlencoded"))
                // If it's an application/x-www-form-urlencoded request, create a StandardFormBody
                body = new StandardFormBody(exchange.getRequestBody());
            else
                body = JsonBody.parseOrNull(exchange.getRequestBody()); // If it's neither multipart nor form data, attempt to parse it as JSON
            return body; // Return the obtained body
        } catch (Exception ignored) {
        }
        return null; // Return null if no body exists or an error occurred
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
    public HttpMethod getRequestMethod() {
        return HttpMethod.parse(exchange.getRequestMethod());
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
