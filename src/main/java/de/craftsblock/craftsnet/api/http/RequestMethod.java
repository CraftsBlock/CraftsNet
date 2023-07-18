package de.craftsblock.craftsnet.api.http;

import java.util.Arrays;

public enum RequestMethod {

    ALL("POST", "GET", "PUT", "DELETE", "PATCH", "HEAD"),
    POST,
    GET,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    UNKNOWN;

    final String[] methods;

    RequestMethod(String... methods) {
        this.methods = methods;
    }

    public String[] getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        if (this.equals(RequestMethod.ALL))
            return String.join("|", methods);
        return super.toString();
    }

    public static RequestMethod parse(String method) {
        for (RequestMethod r : RequestMethod.values())
            if (r.toString().equalsIgnoreCase(method.trim()))
                return r;
        return UNKNOWN;
    }

    public static String[] convert(RequestMethod... methods) {
        if (Arrays.asList(methods).contains(RequestMethod.ALL))
            return RequestMethod.ALL.getMethods();
        return Arrays.stream(methods)
                .filter(method -> !method.equals(RequestMethod.UNKNOWN) && !method.equals(RequestMethod.ALL))
                .map(RequestMethod::toString)
                .toArray(String[]::new);
    }

    public static String asString(RequestMethod... methods) {
        return String.join("|", convert(methods));
    }

}
