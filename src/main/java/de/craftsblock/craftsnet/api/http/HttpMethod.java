package de.craftsblock.craftsnet.api.http;

import de.craftsblock.craftsnet.api.http.annotations.Route;

import java.util.Arrays;
import java.util.Objects;

/**
 * The RequestMethod enum represents the different HTTP request methods, such as POST, GET, PUT, DELETE, PATCH, and HEAD.
 * It also includes the UNKNOWN value for unrecognized methods.
 * The enum provides methods to parse and convert these methods to strings and arrays for use in HTTP request handling.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
 * @see Route
 * @since 1.0.0-SNAPSHOT
 */
public enum HttpMethod {

    ALL("POST", "GET", "PUT", "DELETE", "PATCH"), // Represents all request methods
    ALL_RAW("POST", "GET", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"), // Represents all request methods + raw request methods
    CONNECT(true, true), // Represents the CONNECT request method
    POST(true, true), // Represents the POST request method
    GET(false, true), // Represents the GET request method
    PUT(true, true), // Represents the PUT request method
    DELETE(true, true), // Represents the DELETE request method
    PATCH(true, true), // Represents the PATCH request method
    HEAD(false, false), // Represents the HEAD request method
    OPTIONS(true, true), // Represents the OPTIONS request method
    TRACE(false, true), // Represents the TRACE request method
    UNKNOWN(false, true); // Represents an unrecognized request method

    final String[] methods;
    final boolean requestBody;
    final boolean responseBody;

    /**
     * Constructor to assign the request methods to each enum value
     *
     * @param methods optional set of request method names. (Only used by {@link HttpMethod#ALL})
     */
    HttpMethod(String... methods) {
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
    public String[] getMethods() {
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
        if (this.equals(HttpMethod.ALL) || this.equals(HttpMethod.ALL_RAW))
            return String.join("|", methods);
        return super.toString();
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
     * Convert an array of RequestMethod enum values to an array of strings representing the request methods.
     * If the array contains the ALL value, it returns all methods as an array.
     * It filters out UNKNOWN and ALL values from the array during conversion.
     *
     * @param methods The array of RequestMethod enum values.
     * @return The array of strings representing the request methods.
     */
    public static String[] convert(HttpMethod... methods) {
        return Arrays.stream(methods)
                .filter(Objects::nonNull)
                .filter(method -> !method.equals(UNKNOWN))
                .distinct()
                .flatMap(method -> switch (method) {
                    case ALL, ALL_RAW -> Arrays.stream(method.getMethods());
                    default -> Arrays.stream(new String[]{method.name()});
                })
                .filter(Objects::nonNull)
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * Convert an array of RequestMethod enum values to a single string representation of request methods.
     * The string contains the request methods separated by the "|" character.
     *
     * @param methods The array of RequestMethod enum values.
     * @return The string representation of request methods separated by "|".
     */
    public static String asString(HttpMethod... methods) {
        return String.join("|", convert(methods));
    }

}
