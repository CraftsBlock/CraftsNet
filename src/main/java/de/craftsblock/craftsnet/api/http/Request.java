package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.http.body.Body;
import de.craftsblock.craftsnet.api.http.cookies.Cookie;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The Request class represents an incoming HTTP request received by the web server.
 * It encapsulates information related to the request, such as headers, query parameters, cookies, and request body.
 * <p>
 * The class is responsible for parsing and providing access to various elements of the request, making it easier for
 * request handlers to process and respond to the incoming requests.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.5.2
 * @see Exchange
 * @since 1.0.0-SNAPSHOT
 */
public class Request implements AutoCloseable, RequireAble {

    private final CraftsNet craftsNet;
    private final HttpExchange httpExchange;
    private final Headers headers;
    private final String domain;
    private final HttpMethod httpMethod;
    private final String rawUrl;
    private final String url;
    private final Json queryParams = JsonParser.parse("{}");
    private final ConcurrentHashMap<String, Cookie> cookies = new ConcurrentHashMap<>();
    private final String ip;

    private Exchange exchange;

    private File bodyLocation;
    private Collection<RouteRegistry.EndpointMapping> routes;
    private boolean closed = false;

    /**
     * Constructs a new Request object.
     *
     * @param craftsNet    The CraftsNet instance to which the request was made.
     * @param httpExchange The HttpExchange object representing the incoming HTTP request.
     * @param headers      The headers object representing the headers of the incoming http request.
     * @param url          The query string extracted from the request URI.
     * @param ip           The IP address of the client sending the request.
     * @param domain       The domain used to make the http request.
     * @param httpMethod   The http method used to access the route.
     */
    public Request(CraftsNet craftsNet, HttpExchange httpExchange, Headers headers, String url, String ip, String domain, HttpMethod httpMethod) {
        this.craftsNet = craftsNet;
        this.httpExchange = httpExchange;
        this.headers = headers;
        this.rawUrl = url;
        this.ip = ip;
        this.domain = domain;
        this.httpMethod = httpMethod;

        // Extract relevant information from the incoming request.
        String[] urlStripped = url.split("\\?");
        String query = (urlStripped.length == 2 ? urlStripped[1] : "");
        Arrays.stream(query.split("&")).forEach(pair -> {
            String[] stripped = pair.split("=", 2);
            queryParams.set(stripped[0], stripped.length == 2 ? stripped[1] : "");
        });
        this.url = urlStripped[0];

        for (Cookie cookie : parseCookies(headers))
            this.cookies.put(cookie.getName(), cookie);
        retrieveBody();
    }

    /**
     * Starts retrieving the request body.
     */
    private void retrieveBody() {
        InputStream input = httpExchange.getRequestBody();
        if (input == null) return;
        try {
            bodyLocation = craftsNet.fileHelper().createTempFile("craftsnet_", ".body").toFile();
            bodyLocation.deleteOnExit();
            try (FileOutputStream destination = new FileOutputStream(bodyLocation, true)) {

                byte[] buffer = new byte[2048];
                int read;
                while ((read = input.read(buffer)) != -1) destination.write(buffer, 0, read);
                destination.flush();
                Arrays.fill(buffer, (byte) 0);

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (bodyLocation.length() <= 0) bodyLocation.delete();
    }

    /**
     * Closes the Request object, releasing associated resources such as the HttpExchange and request headers.
     * If request bodies are present, it is also closed all of them to free any related resources.
     *
     * @throws Exception If an error occurs while closing the Request object or its associated resources.
     */
    @Override
    public void close() throws Exception {
        if (bodyLocation != null && bodyLocation.exists()) bodyLocation.delete();
        cookies.clear();
        if (routes != null) routes.clear();
        Body.cleanUp(this);
        closed = true;
    }

    /**
     * Gets if the request has been closed.
     *
     * @return true if the request was closed, false otherwise.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Sets the {@link Exchange} managing this request.
     *
     * @param exchange The {@link Exchange} managing the request
     */
    protected void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Gets the {@link Exchange} managing this request.
     *
     * @return The {@link Exchange} managing this request.
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Gets the instance of CraftsNet to which the request was made.
     *
     * @return The CraftsNet instance.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

    /**
     * Gets the domain through which the route was accessed.
     *
     * @return The domain which is used.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Gets the http method which is used to access this route.
     *
     * @return The http method which is used.
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Gets the raw url which is not trimmed and therefore contains anything except the domain
     *
     * @return The raw url of the request.
     */
    public String getRawUrl() {
        return rawUrl;
    }

    /**
     * Gets the trimmed url which only includes the location of the resource.
     *
     * @return The trimmed url of the request.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets all query parameters stored in a map of the request.
     *
     * @return The mapped query parameters.
     */
    public Map<String, String> getQueryParams() {
        ConcurrentHashMap<String, String> queryParams = new ConcurrentHashMap<>();
        this.queryParams.keySet().forEach(key -> queryParams.put(key, this.queryParams.getString(key)));
        return queryParams;
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
    public String retrieveParam(@NotNull String key) {
        if (hasParam(key))
            return queryParams.getString(key);
        return null;
    }

    /**
     * Retrieves the value of the specified query parameter from the request or return a fallback value if it is not present on this request.
     *
     * @param key      The key of the query parameter to retrieve.
     * @param fallback The fallback value, if the desired query parameter is not present.
     * @return The value of the specified query parameter, or null if the parameter is not found.
     */
    @NotNull
    public String retrieveParam(@NotNull String key, @NotNull String fallback) {
        return hasParam(key) ? Objects.requireNonNull(retrieveParam(key)) : fallback;
    }

    /**
     * Gets all cookies stored in a map of the request.
     *
     * @return The mapped view of all cookies.
     */
    public Map<String, Cookie> getCookies() {
        return Collections.unmodifiableMap(this.cookies);
    }

    /**
     * Checks if the request contains the specified cookie.
     *
     * @param key The name of the cookie to check.
     * @return True if the request contains the specified cookie, false otherwise.
     */
    public boolean hasCookie(String key) {
        return cookies.containsKey(key);
    }

    /**
     * Retrieves the specified cookie from the request.
     *
     * @param key The name of the cookie to retrieve.
     * @return The specified cookie, or null if the cookie is not found.
     */
    @Nullable
    public Cookie retrieveCookie(@NotNull String key) {
        if (hasCookie(key))
            return cookies.get(key);
        return null;
    }

    /**
     * Retrieves the specified cookie from the request or return a fallback cookie if it is not present on this request.
     *
     * @param key      The name of the cookie to retrieve.
     * @param fallback The fallback value, if the desired cookie is not present.
     * @return The specified cookie, or null if the cookie is not found.
     */
    @NotNull
    public Cookie retrieveCookie(@NotNull String key, @NotNull Cookie fallback) {
        return hasCookie(key) ? Objects.requireNonNull(retrieveCookie(key)) : fallback;
    }

    /**
     * Retrieves the content type form the http request.
     *
     * @return The content type.
     */
    public String getContentType() {
        return getHeader("Content-Type");
    }

    /**
     * Retrieves the matched routes mapping for the request.
     *
     * @return The RouteMapping objects representing the matched route, or null if no route is matched.
     */
    @Nullable
    public Collection<RouteRegistry.EndpointMapping> getRoutes() {
        return routes;
    }

    /**
     * Checks if the HTTP request has a request body.
     *
     * @return {@code true} if a request body exists, otherwise {@code false}.
     */
    public boolean hasBody() {
        if (isClosed()) throw new IllegalStateException("Could not retrieve the body as this request is already closed!");
        return bodyLocation != null && bodyLocation.exists() && bodyLocation.length() >= 1;
    }

    /**
     * Retrieves the HTTP request body, if it exists.
     *
     * @return The HTTP request body as a {@code Body} object, or {@code null} if no body exists or an error occurs.
     */
    public Body getBody() {
        // Abort if request is closed or the body is not present.
        if (isClosed()) throw new IllegalStateException("Could not retrieve the body as this request is already closed!");
        if (!hasBody()) throw new IllegalStateException("Could not retrieve the body as it does not exists!");
        // Returns a blank body with only the standard functionality.
        return new Body(this) {
        };
    }

    /**
     * Retrieves a new instance of an input stream containing the request body.
     *
     * @return The input stream containing the body.
     * @throws FileNotFoundException If the save file which contains the bytes of the body was not found.
     */
    public InputStream getRawBody() throws FileNotFoundException {
        if (isClosed()) throw new IllegalStateException("Could not retrieve the body as this request is already closed!");
        return new FileInputStream(bodyLocation);
    }

    /**
     * Gets all the headers which are present on the request.
     *
     * @return The headers object including the headers.
     */
    public Headers getHeaders() {
        return headers;
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
        return HttpMethod.parse(httpExchange.getRequestMethod());
    }

    /**
     * Retrieves the HttpExchange object representing the incoming HTTP request.
     *
     * @return The HttpExchange object.
     */
    public HttpExchange unsafe() {
        return httpExchange;
    }

    /**
     * Sets the matched routes mapping for the request.
     *
     * @param routes The RouteMapping objects representing the matched route.
     */
    protected void setRoutes(Collection<RouteRegistry.EndpointMapping> routes) {
        this.routes = new ArrayList<>(routes);
    }

    /**
     * Parses the "Cookie" header from the given HTTP headers and returns a collection of Cookie objects.
     * <p>
     * This method extracts cookie information from the "Cookie" header and constructs Cookie objects
     * for each cookie found. It handles attributes such as Path, Domain, Expires, SameSite, Secure, and HttpOnly.
     *
     * @param headers The HTTP headers from which to parse cookies
     * @return A ConcurrentLinkedQueue containing all parsed cookies
     */
    protected static ConcurrentLinkedQueue<Cookie> parseCookies(Headers headers) {
        ConcurrentLinkedQueue<Cookie> cookies = new ConcurrentLinkedQueue<>();
        if (headers.containsKey("cookie"))
            Arrays.stream(headers.getFirst("cookie").split("; ")).forEach(pair -> {
                String[] args = pair.split(";");
                if (args.length < 1) return;
                String[] nameValue = args[0].split("=", 2);

                Cookie cookie = new Cookie(nameValue[0], nameValue.length == 2 ? nameValue[1] : "");
                for (int i = 0; i < args.length; i++) {
                    if (i == 0) continue;
                    String[] arg = args[i].split("=");
                    if (arg.length == 0) continue;
                    cookie.setFlag(arg[0], arg.length == 2 ? arg[1] : null);
                }
                cookies.add(cookie);
            });
        return cookies;
    }

}
