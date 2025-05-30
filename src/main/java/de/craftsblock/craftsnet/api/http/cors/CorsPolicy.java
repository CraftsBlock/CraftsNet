package de.craftsblock.craftsnet.api.http.cors;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.Response;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Cross-Origin Resource Sharing (CORS) policy, which defines how requests
 * from other origins (domains) can interact with the server's resources.
 * This class allows fine-grained control over which origins, methods, and headers are allowed
 * in cross-origin requests, and whether credentials are permitted.
 * <p>
 * The policy can be customized to either allow or disallow specific origins, http methods,
 * headers, and control headers exposure.
 * <p>
 * <b>Important: </b>The cors policy is applied to an exchange automatically. You don't need to take further actions!
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.1.0
 */
public class CorsPolicy {

    /**
     * The http header that specifies which origin sites are allowed to access the resource.
     */
    public static final String ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";

    /**
     * The http header that indicates which http methods are permitted when accessing the resource.
     */
    public static final String ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";

    /**
     * The http header that defines which custom request headers are allowed during a CORS request.
     */
    public static final String ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";

    /**
     * The http header that specifies which headers are safe to expose to the client.
     */
    public static final String EXPOSE_HEADERS_HEADER = "Access-Control-Expose-Headers";

    /**
     * The http header that indicates whether the response to the request can be exposed
     * when the credentials flag is true.
     */
    public static final String ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";

    /**
     * The http header that indicates how long the results of a preflight request can be cached.
     */
    public static final String MAX_AGE_HEADER = "Access-Control-Max-Age";

    /**
     * The http header sent by browsers in preflight requests to indicate which custom headers
     * will be included in the actual request.
     */
    public static final String REQUEST_HEADERS_HEADER = "Access-Control-Request-Headers";

    private boolean allowAllOrigins = false;
    private boolean allowAllMethods = false;
    private boolean allowAllHeaders = false;
    private boolean overrideCredentials, allowCredentials = false;

    private int controlMaxAge = -1;

    private final List<String> allowedOrigins = new ArrayList<>();
    private final List<HttpMethod> allowedMethods = new ArrayList<>();
    private final List<String> allowedHeaders = new ArrayList<>();
    private final List<String> exposedHeaders = new ArrayList<>();

    /**
     * Creates a new, empty CORS policy. By default, all origins, methods, and headers are disallowed,
     * and credentials are not allowed. Specific settings can be configured using the provided methods.
     */
    public CorsPolicy() {
    }

    /**
     * Allows all origins for cross-origin requests.
     * Once enabled, any domain can make requests to the server.
     */
    public void allowAllOrigins() {
        allowAllOrigins = true;
    }

    /**
     * Disallows all origins, restricting cross-origin requests to specific origins if set.
     */
    public void disallowAllOrigins() {
        allowAllOrigins = false;
    }

    /**
     * Allows all http methods (e.g., GET, POST, DELETE) for cross-origin requests.
     */
    public void allowAllMethods() {
        allowAllMethods = true;
    }

    /**
     * Disallows all http methods, limiting cross-origin requests to only specific methods.
     */
    public void disallowAllMethods() {
        allowAllMethods = false;
    }

    /**
     * Allows all http headers for cross-origin requests, meaning any header can be sent from the client.
     */
    public void allowAllHeaders() {
        allowAllHeaders = true;
    }

    /**
     * Disallows all headers, requiring cross-origin requests to specify only permitted headers.
     */
    public void disallowAllHeaders() {
        allowAllHeaders = false;
    }

    /**
     * Allows credentials (such as cookies or authentication data) in cross-origin requests.
     */
    public void allowCredentials() {
        overrideCredentials = allowCredentials = true;
    }

    /**
     * Disallows credentials in cross-origin requests.
     */
    public void disallowCredentials() {
        overrideCredentials = true;
        allowCredentials = false;
    }

    /**
     * Adds one or more origins to the list of allowed origins for cross-origin requests.
     *
     * @param origins The origins to be allowed.
     */
    public void addAllowedOrigin(String... origins) {
        add(allowedOrigins, origins);
    }

    /**
     * Removes one or more origins from the list of allowed origins.
     *
     * @param origins The origins to be removed.
     */
    public void removeAllowedOrigin(String... origins) {
        remove(allowedOrigins, origins);
    }

    /**
     * Adds one or more http methods to the list of allowed methods for cross-origin requests.
     *
     * @param methods The http methods to be allowed.
     */
    public void addAllowedMethod(HttpMethod... methods) {
        add(allowedMethods, methods);
    }

    /**
     * Removes one or more http methods from the list of allowed methods.
     *
     * @param methods The http methods to be removed.
     */
    public void removeAllowedOrigin(HttpMethod... methods) {
        remove(allowedMethods, methods);
    }

    /**
     * Adds one or more http headers to the list of allowed headers for cross-origin requests.
     *
     * @param headers The http headers to be allowed.
     */
    public void addAllowedHeader(String... headers) {
        add(allowedHeaders, headers);
    }

    /**
     * Removes one or more http headers from the list of allowed headers.
     *
     * @param headers The http headers to be removed.
     */
    public void removeAllowedHeader(String... headers) {
        remove(allowedHeaders, headers);
    }

    /**
     * Adds one or more http headers to the list of headers exposed to the client.
     * Exposed headers can be accessed by the client application.
     *
     * @param headers The http headers to be exposed.
     */
    public void addExposedHeader(String... headers) {
        add(exposedHeaders, headers);
    }

    /**
     * Removes one or more headers from the list of exposed headers.
     *
     * @param headers The headers to be removed.
     */
    public void removeExposedHeader(String... headers) {
        remove(exposedHeaders, headers);
    }

    /**
     * Adds values to a list, ensuring that duplicates are not added.
     *
     * @param list   The list to which values should be added.
     * @param values The values to be added to the list.
     * @param <T>    The type of elements in the list.
     * @param <L>    The type of the list.
     */
    @SafeVarargs
    private <T, L extends List<T>> void add(L list, T... values) {
        for (T value : values)
            if (!list.contains(value)) list.add(value);
    }

    /**
     * Removes values from a list.
     *
     * @param list   The list from which values should be removed.
     * @param values The values to be removed from the list.
     * @param <T>    The type of elements in the list.
     * @param <L>    The type of the list.
     */
    @SafeVarargs
    private <T, L extends List<T>> void remove(L list, T... values) {
        for (T value : values)
            list.remove(value);
    }

    /**
     * Sets the maximum age (in seconds) for how long the results of a preflight request
     * can be cached by the client.
     *
     * @param controlMaxAge The max age in seconds.
     */
    public void setControlMaxAge(@Range(from = -1, to = Integer.MAX_VALUE) int controlMaxAge) {
        this.controlMaxAge = controlMaxAge;
    }

    /**
     * Retrieves the list of allowed origins for cross-origin requests.
     *
     * @return A list of allowed origins.
     */
    public @Unmodifiable List<String> getAllowedOrigins() {
        return Collections.unmodifiableList(allowedOrigins);
    }

    /**
     * Retrieves the list of allowed http methods for cross-origin requests.
     *
     * @return A list of allowed http methods.
     */
    public @Unmodifiable List<HttpMethod> getAllowedMethods() {
        return Collections.unmodifiableList(allowedMethods);
    }

    /**
     * Retrieves the list of allowed http headers for cross-origin requests.
     *
     * @return A list of allowed http headers.
     */
    public @Unmodifiable List<String> getAllowedHeaders() {
        return Collections.unmodifiableList(allowedHeaders);
    }

    /**
     * Retrieves the list of exposed headers that the client can access.
     *
     * @return A list of exposed headers.
     */
    public @Unmodifiable List<String> getExposedHeaders() {
        return Collections.unmodifiableList(exposedHeaders);
    }

    /**
     * Checks if all origins are allowed for cross-origin requests.
     *
     * @return true if all origins are allowed, false otherwise.
     */
    public boolean isAllowAllOrigins() {
        return allowAllOrigins;
    }

    /**
     * Checks if all http methods are allowed for cross-origin requests.
     *
     * @return true if all methods are allowed, false otherwise.
     */
    public boolean isAllowAllMethods() {
        return allowAllMethods;
    }

    /**
     * Checks if all http headers are allowed for cross-origin requests.
     *
     * @return true if all headers are allowed, false otherwise.
     */
    public boolean isAllowAllHeaders() {
        return allowAllHeaders;
    }

    /**
     * Checks if credentials (such as cookies) are allowed in cross-origin requests.
     *
     * @return true if credentials are allowed, false otherwise.
     */
    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    /**
     * Retrieves the maximum age in seconds for how long preflight request results
     * can be cached by the client.
     *
     * @return The max age, or -1 if not set.
     */
    public int getControlMaxAge() {
        return controlMaxAge;
    }

    /**
     * Resets the CORS policy by disabling all settings and restoring it to its default state.
     */
    public void disable() {
        this.update(new CorsPolicy());
    }

    /**
     * Updates the current CORS policy by copying the settings from another policy.
     *
     * @param policy The policy whose settings should be copied.
     */
    public void update(CorsPolicy policy) {
        this.allowAllOrigins = policy.allowAllOrigins;
        this.allowAllMethods = policy.allowAllMethods;
        this.allowAllHeaders = policy.allowAllHeaders;

        this.overrideCredentials = policy.overrideCredentials;
        this.allowCredentials = policy.allowCredentials;

        this.controlMaxAge = policy.controlMaxAge;

        this.allowedOrigins.clear();
        this.allowedMethods.clear();
        this.allowedHeaders.clear();
        this.exposedHeaders.clear();

        this.allowedOrigins.addAll(policy.allowedOrigins);
        this.allowedMethods.addAll(policy.allowedMethods);
        this.allowedHeaders.addAll(policy.allowedHeaders);
        this.exposedHeaders.addAll(policy.exposedHeaders);
    }

    /**
     * Applies the current CORS policy to an {@link Exchange} object by adding appropriate
     * headers to the response based on the allowed origins, methods, headers, and credentials.
     *
     * @param exchange The exchange on which to apply the CORS policy.
     */
    @ApiStatus.Internal
    public void apply(Exchange exchange) {
        Request request = exchange.request();
        Response response = exchange.response();

        String origin = getOrigin(request, false);
        if (allowAllOrigins) response.setHeader(ALLOW_ORIGIN_HEADER, origin);
        else if (!allowedOrigins.isEmpty() && allowedOrigins.contains(origin.replaceFirst(".*://", "")))
            response.setHeader(ALLOW_ORIGIN_HEADER, origin);
        else if (!allowedOrigins.isEmpty())
            response.setHeader(ALLOW_ORIGIN_HEADER, allowedOrigins.get(0));

        if (allowAllMethods) response.setHeader(ALLOW_METHODS_HEADER, HttpMethod.join(", ", HttpMethod.ALL_RAW));
        else if (!allowedMethods.isEmpty())
            response.setHeader(ALLOW_METHODS_HEADER, HttpMethod.join(", ", allowedMethods.toArray(HttpMethod[]::new)));

        if (allowAllHeaders) {

            if (request.hasHeader(REQUEST_HEADERS_HEADER))
                response.setHeader(ALLOW_HEADERS_HEADER, String.join(", ", request.getHeaders().get(REQUEST_HEADERS_HEADER)));
            else response.setHeader(ALLOW_HEADERS_HEADER, "*");

        } else if (!allowedHeaders.isEmpty())
            response.setHeader(ALLOW_HEADERS_HEADER, String.join(", ", allowedHeaders));

        if (!exposedHeaders.isEmpty())
            response.setHeader(EXPOSE_HEADERS_HEADER, String.join(", ", exposedHeaders));

        if (overrideCredentials) response.setHeader(ALLOW_CREDENTIALS_HEADER, "" + allowCredentials);
        if (controlMaxAge >= 0) response.setHeader(MAX_AGE_HEADER, "" + controlMaxAge);
    }

    /**
     * Retrieves the origin (domain or URL) from a given {@link Request} object.
     * This method defaults to stripping the protocol from the origin URL.
     *
     * @param request the {@link Request} object from which to extract the origin.
     * @return the origin as a {@link String}, either with or without the protocol depending on the configuration.
     */
    private String getOrigin(Request request) {
        return getOrigin(request, true);
    }

    /**
     * Retrieves the origin (domain or URL) from a given {@link Request} object.
     * Depending on the {@code strip} flag, the protocol can be removed from the origin URL.
     *
     * <p>If the request contains the "Origin" header, that value is used. If the {@code strip} flag is {@code true},
     * the protocol is removed from the origin. If the header is not present, the method constructs the origin
     * using the domain and the protocol based on the web server's SSL status.</p>
     *
     * @param request the {@link Request} object from which to extract the origin.
     * @param strip   whether to remove the protocol from the origin.
     * @return the origin as a {@link String}, either with or without the protocol depending on the {@code strip} flag.
     */
    private String getOrigin(Request request, boolean strip) {
        if (request.hasHeader("Origin")) {
            String origin = request.getHeader("Origin");

            if (strip) return origin.replaceFirst(".*://", "");
            return origin;
        }

        String domain = request.getDomain();
        if (strip) return domain;
        return "http" + (request.getCraftsNet().webServer().isSSL() ? "s" : "") + "://" + domain;
    }

}
