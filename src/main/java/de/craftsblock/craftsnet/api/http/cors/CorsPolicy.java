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
 * The policy can be customized to either allow or disallow specific origins, HTTP methods,
 * headers, and control headers exposure.
 * <p>
 * <b>Important: </b>The cors policy is applied to an exchange automatically. You don't need to take further actions!
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.0.7
 */
public class CorsPolicy {

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
    public synchronized void allowAllOrigins() {
        allowAllOrigins = true;
    }

    /**
     * Disallows all origins, restricting cross-origin requests to specific origins if set.
     */
    public synchronized void disallowAllOrigins() {
        allowAllOrigins = false;
    }

    /**
     * Allows all HTTP methods (e.g., GET, POST, DELETE) for cross-origin requests.
     */
    public synchronized void allowAllMethods() {
        allowAllMethods = true;
    }

    /**
     * Disallows all HTTP methods, limiting cross-origin requests to only specific methods.
     */
    public synchronized void disallowAllMethods() {
        allowAllMethods = false;
    }

    /**
     * Allows all HTTP headers for cross-origin requests, meaning any header can be sent from the client.
     */
    public synchronized void allowAllHeaders() {
        allowAllHeaders = true;
    }

    /**
     * Disallows all headers, requiring cross-origin requests to specify only permitted headers.
     */
    public synchronized void disallowAllHeaders() {
        allowAllHeaders = false;
    }

    /**
     * Allows credentials (such as cookies or authentication data) in cross-origin requests.
     */
    public synchronized void allowCredentials() {
        overrideCredentials = allowCredentials = true;
    }

    /**
     * Disallows credentials in cross-origin requests.
     */
    public synchronized void disallowCredentials() {
        overrideCredentials = true;
        allowCredentials = false;
    }

    /**
     * Adds one or more origins to the list of allowed origins for cross-origin requests.
     *
     * @param origins The origins to be allowed.
     */
    public synchronized void addAllowedOrigin(String... origins) {
        add(allowedOrigins, origins);
    }

    /**
     * Removes one or more origins from the list of allowed origins.
     *
     * @param origins The origins to be removed.
     */
    public synchronized void removeAllowedOrigin(String... origins) {
        remove(allowedOrigins, origins);
    }

    /**
     * Adds one or more HTTP methods to the list of allowed methods for cross-origin requests.
     *
     * @param methods The HTTP methods to be allowed.
     */
    public synchronized void addAllowedMethod(HttpMethod... methods) {
        add(allowedMethods, methods);
    }

    /**
     * Removes one or more HTTP methods from the list of allowed methods.
     *
     * @param methods The HTTP methods to be removed.
     */
    public synchronized void removeAllowedOrigin(HttpMethod... methods) {
        remove(allowedMethods, methods);
    }

    /**
     * Adds one or more HTTP headers to the list of allowed headers for cross-origin requests.
     *
     * @param headers The HTTP headers to be allowed.
     */
    public synchronized void addAllowedHeader(String... headers) {
        add(allowedHeaders, headers);
    }

    /**
     * Removes one or more HTTP headers from the list of allowed headers.
     *
     * @param headers The HTTP headers to be removed.
     */
    public synchronized void removeAllowedHeader(String... headers) {
        remove(allowedHeaders, headers);
    }

    /**
     * Adds one or more HTTP headers to the list of headers exposed to the client.
     * Exposed headers can be accessed by the client application.
     *
     * @param headers The HTTP headers to be exposed.
     */
    public synchronized void addExposedHeader(String... headers) {
        add(exposedHeaders, headers);
    }

    /**
     * Removes one or more headers from the list of exposed headers.
     *
     * @param headers The headers to be removed.
     */
    public synchronized void removeExposedHeader(String... headers) {
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
     * Retrieves the list of allowed HTTP methods for cross-origin requests.
     *
     * @return A list of allowed HTTP methods.
     */
    public @Unmodifiable List<HttpMethod> getAllowedMethods() {
        return Collections.unmodifiableList(allowedMethods);
    }

    /**
     * Retrieves the list of allowed HTTP headers for cross-origin requests.
     *
     * @return A list of allowed HTTP headers.
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
     * Checks if all HTTP methods are allowed for cross-origin requests.
     *
     * @return true if all methods are allowed, false otherwise.
     */
    public boolean isAllowAllMethods() {
        return allowAllMethods;
    }

    /**
     * Checks if all HTTP headers are allowed for cross-origin requests.
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

        if (allowAllOrigins) response.setHeader("Access-Control-Allow-Origin", "*");
        else if (allowedOrigins.isEmpty() && allowedOrigins.contains(request.getDomain()))
            response.setHeader("Access-Control-Allow-Origin", "http" + (response.getCraftsNet().webServer().isSSL() ? "s" : "") + "://" + request.getDomain());

        if (allowAllMethods) response.setHeader("Access-Control-Allow-Methods", String.join(", ", HttpMethod.ALL_RAW.getMethods()));
        else if (!allowedMethods.isEmpty())
            response.setHeader("Access-Control-Allow-Methods", String.join(", ", allowedMethods.parallelStream().map(Enum::name).toList()));

        if (allowAllHeaders) response.setHeader("Access-Control-Allow-Headers", "*");
        else if (!allowedHeaders.isEmpty())
            response.setHeader("Access-Control-Allow-Headers", String.join(", ", String.join(", ", allowedHeaders)));

        if (!exposedHeaders.isEmpty())
            response.setHeader("Access-Control-Expose-Headers", String.join(", ", String.join(", ", exposedHeaders)));

        if (overrideCredentials) response.setHeader("Access-Control-Allow-Credentials", "" + allowCredentials);
        if (controlMaxAge >= 0) response.setHeader("Access-Control-Max-Age", "" + controlMaxAge);
    }

}
