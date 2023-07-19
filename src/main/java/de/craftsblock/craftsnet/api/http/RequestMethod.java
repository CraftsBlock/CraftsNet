package de.craftsblock.craftsnet.api.http;

import java.util.Arrays;

/**
 * The RequestMethod enum represents the different HTTP request methods, such as POST, GET, PUT, DELETE, PATCH, and HEAD.
 * It also includes the UNKNOWN value for unrecognized methods.
 * The enum provides methods to parse and convert these methods to strings and arrays for use in HTTP request handling.
 *
 * @author CraftsBlock
 * @see Route
 * @since 1.0.0
 */
public enum RequestMethod {

    ALL("POST", "GET", "PUT", "DELETE", "PATCH", "HEAD"), // Represents all request methods
    POST, // Represents the POST request method
    GET, // Represents the GET request method
    PUT, // Represents the PUT request method
    DELETE, // Represents the DELETE request method
    PATCH, // Represents the PATCH request method
    HEAD, // Represents the HEAD request method
    UNKNOWN; // Represents an unrecognized request method

    final String[] methods;

    /**
     * Constructor to assign the request methods to each enum value
     * @param methods optional set of request method names. (Only used by {@link RequestMethod#ALL})
     */
    RequestMethod(String... methods) {
        this.methods = methods;
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
     * Get the string representation of the enum value. For the ALL value, it returns the concatenated string of all methods.
     * For other values, it returns the default string representation of the enum value.
     *
     * @return The string representation of the enum value.
     */
    @Override
    public String toString() {
        if (this.equals(RequestMethod.ALL))
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
    public static RequestMethod parse(String method) {
        for (RequestMethod r : RequestMethod.values())
            if (r.toString().equalsIgnoreCase(method.trim()))
                return r;
        return UNKNOWN;
    }

    /**
     * Convert an array of RequestMethod enum values to an array of strings representing the request methods.
     * If the array contains the ALL value, it returns all methods as an array.
     * It filters out UNKNOWN and ALL values from the array during conversion.
     *
     * @param methods The array of RequestMethod enum values.
     * @return The array of strings representing the request methods.
     */
    public static String[] convert(RequestMethod... methods) {
        if (Arrays.asList(methods).contains(RequestMethod.ALL))
            return RequestMethod.ALL.getMethods();
        return Arrays.stream(methods)
                .filter(method -> !method.equals(RequestMethod.UNKNOWN) && !method.equals(RequestMethod.ALL))
                .map(RequestMethod::toString)
                .toArray(String[]::new);
    }

    /**
     * Convert an array of RequestMethod enum values to a single string representation of request methods.
     * The string contains the request methods separated by the "|" character.
     *
     * @param methods The array of RequestMethod enum values.
     * @return The string representation of request methods separated by "|".
     */
    public static String asString(RequestMethod... methods) {
        return String.join("|", convert(methods));
    }

}
