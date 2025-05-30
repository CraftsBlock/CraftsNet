package de.craftsblock.craftsnet.api.http;

import de.craftsblock.craftsnet.api.http.annotations.Route;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * The RequestMethod enum represents the different HTTP request methods, such as POST, GET, PUT, DELETE, PATCH, and HEAD.
 * It also includes the UNKNOWN value for unrecognized methods.
 * The enum provides methods to parse and convert these methods to strings and arrays for use in HTTP request handling.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.0
 * @see Route
 * @since 1.0.0-SNAPSHOT
 */
public enum HttpMethod {

    /**
     * Represents the CONNECT request method
     */
    CONNECT(true, true),

    /**
     * Represents the POST request method
     */
    POST(true, true),

    /**
     * Represents the GET request method
     */
    GET(false, true),

    /**
     * Represents the PUT request method
     */
    PUT(true, true),

    /**
     * Represents the DELETE request method
     */
    DELETE(true, true),

    /**
     * Represents the PATCH request method
     */
    PATCH(true, true),

    /**
     * Represents the HEAD request method
     */
    HEAD(false, false),

    /**
     * Represents the OPTIONS request method
     */
    OPTIONS(true, true),

    /**
     * Represents the TRACE request method
     */
    TRACE(false, true),

    /**
     * Represents an unrecognized request method
     */
    UNKNOWN(false, true),

    /**
     * Represents all request methods
     */
    ALL(POST, GET, PUT, DELETE, PATCH),

    /**
     * Represents all request methods + raw request methods
     */
    ALL_RAW(HttpMethod.normalize(ALL, HEAD, OPTIONS)),

    ;

    final HttpMethod[] methods;
    final boolean requestBody;
    final boolean responseBody;

    /**
     * Constructor to assign the request methods to each enum value
     *
     * @param methods optional set of request method names. (Only used by {@link HttpMethod#ALL})
     */
    HttpMethod(HttpMethod... methods) {
        this.methods = methods;
        this.requestBody = this.responseBody = false;
    }

    /**
     * Constructor to assign the properties of requestBody and responseBody for HTTP methods.
     *
     * @param requestBody  specifies if the HTTP method accepts a request body
     * @param responseBody specifies if the HTTP method returns a response body
     */
    HttpMethod(boolean requestBody, boolean responseBody) {
        this.methods = null;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
    }

    /**
     * Get the array of strings containing the request methods for this enum value.
     *
     * @return The array of request methods as strings.
     */
    public HttpMethod[] getMethods() {
        return methods;
    }

    /**
     * Check if the HTTP method allows a request body.
     *
     * @return true if the HTTP method allows a request body, otherwise false.
     */
    public boolean isRequestBodyAble() {
        return requestBody;
    }

    /**
     * Check if the HTTP method allows a response body.
     *
     * @return true if the HTTP method allows a response body, otherwise false.
     */
    public boolean isResponseBodyAble() {
        return responseBody;
    }

    /**
     * Get the string representation of the enum value. For the ALL value, it returns the concatenated string of all methods.
     * For other values, it returns the default string representation of the enum value.
     *
     * @return The string representation of the enum value.
     */
    @Override
    public String toString() {
        return HttpMethod.join(this);
    }

    /**
     * Parse the given HTTP request method string and return the corresponding RequestMethod enum value.
     * If the method string is not recognized, it returns the UNKNOWN value.
     *
     * @param method The HTTP request method as a string.
     * @return The corresponding RequestMethod enum value or UNKNOWN if not recognized.
     */
    public static HttpMethod parse(String method) {
        try {
            return HttpMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * Normalizes the provided array of HttpMethod values by:
     * <ul>
     *     <li>Removing null values</li>
     *     <li>Excluding UNKNOWN methods</li>
     *     <li>Flattening ALL and ALL_RAW into their actual method components</li>
     *     <li>Removing duplicates</li>
     * </ul>
     *
     * @param methods The array of HttpMethod values to normalize
     * @return A normalized array of distinct, valid HttpMethod values
     * @since 3.4.3-SNAPSHOT
     */
    public static HttpMethod[] normalize(HttpMethod @NotNull ... methods) {
        return Arrays.stream(methods)
                .filter(Objects::nonNull)
                .filter(method -> !method.equals(UNKNOWN))
                .distinct()
                .flatMap(method -> switch (method) {
                    case ALL, ALL_RAW -> Arrays.stream(method.getMethods());
                    default -> Arrays.stream(new HttpMethod[]{method});
                })
                .filter(Objects::nonNull)
                .distinct()
                .toArray(HttpMethod[]::new);
    }

    /**
     * Joins the names of the provided {@link HttpMethod} values using the default delimiter "|".
     * The methods are normalized before joining.
     *
     * @param methods The array of HttpMethod values to join.
     * @return A single string of method names separated by the default delimiter.
     * @since 3.4.3-SNAPSHOT
     */
    public static String join(HttpMethod @NotNull ... methods) {
        return HttpMethod.join("|", methods);
    }

    /**
     * Joins the names of the provided {@link HttpMethod} values into a single string using the specified delimiter.
     * The methods are normalized before joining.
     *
     * @param delimiter The string used to separate method names.
     * @param methods   The array of HttpMethod values to join.
     * @return A string of joined method names, or an empty string if methods is null or empty.
     * @since 3.4.3-SNAPSHOT
     */
    public static String join(@NotNull CharSequence delimiter, HttpMethod @NotNull ... methods) {
        if (methods.length == 0) return "";
        return String.join(delimiter, Arrays.stream(normalize(methods)).map(HttpMethod::name).toList());
    }

    /**
     * Convert an array of RequestMethod enum values to an array of strings representing the request methods.
     * If the array contains the ALL value, it returns all methods as an array.
     * It filters out UNKNOWN and ALL values from the array during conversion.
     *
     * @param methods The array of RequestMethod enum values.
     * @return The array of strings representing the request methods.
     * @deprecated in favor of {@link #normalize(HttpMethod...)}.
     */
    @Deprecated(since = "3.4.3-SNAPSHOT", forRemoval = true)
    public static String[] convert(HttpMethod... methods) {
        return Arrays.stream(normalize(methods)).map(HttpMethod::name).toArray(String[]::new);
    }

    /**
     * Convert an array of RequestMethod enum values to a single string representation of request methods.
     * The string contains the request methods separated by the "|" character.
     *
     * @param methods The array of RequestMethod enum values.
     * @return The string representation of request methods separated by "|".
     * @deprecated in favor of {@link #join(HttpMethod...)}.
     */
    @Deprecated(since = "3.4.3-SNAPSHOT", forRemoval = true)
    public static String asString(HttpMethod... methods) {
        return HttpMethod.join(methods);
    }

}
