package de.craftsblock.craftsnet.builder.server.trust;

import com.sun.net.httpserver.Headers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration describing which forwarded headers are trusted
 * and which proxies are allowed to provide forwarded request information.
 * <p>
 * This record provides utilities for selecting configured headers and extracting
 * host and IP information from HTTP request headers. It also allows checking
 * whether a given IP address belongs to a trusted proxy.
 *
 * @param forwardedHostHeaders the ordered list of headers that may provide the original host
 * @param forwardedIpHeaders   the ordered list of headers that may provide the original client IP
 * @param trustedProxies       the list of trusted proxy addresses
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 4.0.0
 */
public record ServerTrust(
        @NotNull @UnmodifiableView List<String> forwardedHostHeaders,
        @NotNull @UnmodifiableView List<String> forwardedIpHeaders,
        @NotNull @UnmodifiableView List<String> trustedProxies
) {

    /**
     * Creates an immutable server trust configuration.
     * <p>
     * Each supplied list is copied using {@link List#copyOf(Collection)}, preventing
     * subsequent modifications to the original lists from affecting this configuration.
     *
     * @param forwardedHostHeaders the headers used to determine the forwarded host
     * @param forwardedIpHeaders   the headers used to determine the forwarded client IP
     * @param trustedProxies       the proxy addresses considered trusted
     * @throws NullPointerException if any of the supplied lists is {@code null}
     */
    public ServerTrust(
            @NotNull List<String> forwardedHostHeaders,
            @NotNull List<String> forwardedIpHeaders,
            @NotNull List<String> trustedProxies
    ) {
        this.forwardedHostHeaders = List.copyOf(
                Objects.requireNonNull(forwardedHostHeaders, "forwardedHostHeaders")
        );
        this.forwardedIpHeaders = List.copyOf(
                Objects.requireNonNull(forwardedIpHeaders, "forwardedIpHeaders")
        );
        this.trustedProxies = List.copyOf(
                Objects.requireNonNull(trustedProxies, "trustedProxies")
        );
    }

    /**
     * Selects the first configured forwarded-host header that is present in the supplied collection.
     *
     * @param headers the available HTTP header names
     * @return the first matching configured header name, or {@code null} if none is present
     */
    public String pickFirstForwardedHostHeader(Collection<String> headers) {
        return this.pickFirstHeader(forwardedHostHeaders, headers);
    }

    /**
     * Selects the first configured forwarded-IP header that is present in the supplied collection.
     *
     * @param headers the available HTTP header names
     * @return the first matching configured header name, or {@code null} if none is present
     */
    public String pickFirstForwardedIpHeader(Collection<String> headers) {
        return this.pickFirstHeader(forwardedIpHeaders, headers);
    }

    /**
     * Searches the configured header names in their configured order and returns
     * the first one contained in the supplied collection.
     *
     * @param trusted the ordered list of configured header names
     * @param headers the available header names
     * @return the first matching header name, or {@code null} if no match exists
     */
    private String pickFirstHeader(List<String> trusted, Collection<String> headers) {
        return trusted.stream()
                .filter(headers::contains)
                .findFirst()
                .orElse(null);
    }

    /**
     * Extracts the host from the supplied HTTP headers.
     * <p>
     * The configured forwarded-host headers are checked first. If none is present,
     * the {@code Host} header is used when available. If neither source provides
     * a host, this method returns {@code null}.
     *
     * @param headers the HTTP request headers
     * @return the extracted host, or {@code null} if no host can be determined
     */
    public String extractHost(Headers headers) {
        return this.extractHost(headers, null);
    }

    /**
     * Extracts the host from the supplied HTTP headers, using a fallback if extraction fails.
     * <p>
     * The first configured forwarded-host header present in the request is preferred.
     * If no configured forwarded-host header is present, the {@code Host} header is
     * used when available. The fallback is returned if the selected source does not
     * yield a host.
     *
     * @param headers  the HTTP request headers
     * @param fallback the value to return if no host can be extracted
     * @return the extracted host, or the fallback value
     */
    public String extractHost(Headers headers, String fallback) {
        String headerName = this.pickFirstForwardedHostHeader(headers.keySet());

        String host;
        if (headerName != null) {
            host = this.extractHost(headers.getFirst(headerName));
        } else if (headers.containsKey("Host")) {
            host = headers.getFirst("Host").split(":")[0];
        } else {
            host = null;
        }

        return host != null ? host : fallback;
    }

    /**
     * Extracts a host name or address from a forwarded header value.
     * <p>
     * If the value contains multiple comma-separated entries, only the first entry
     * is considered. Surrounding whitespace is removed. Bracketed IPv6 addresses
     * are returned without their brackets, while a port is removed from non-IPv6
     * values when the value contains exactly one colon.
     *
     * @param headerValue the raw forwarded header value
     * @return the extracted host, or {@code null} if the value is {@code null}, blank,
     * or produces an empty host
     */
    public String extractHost(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String host = headerValue.split(",")[0].trim();

        // IPv6
        if (host.startsWith("[")) {
            int closingBracket = host.indexOf(']');
            if (closingBracket >= 0) {
                return host.substring(1, closingBracket);
            }
        }

        // Remove port
        int colonIndex = host.lastIndexOf(':');
        if (colonIndex > 0 && host.indexOf(':') == colonIndex) {
            host = host.substring(0, colonIndex);
        }

        return host.isBlank() ? null : host;
    }

    /**
     * Extracts the client IP address from the configured forwarded-IP headers.
     * <p>
     * If no configured forwarded-IP header is present, or its value does not yield
     * an IP address, this method returns {@code null}.
     *
     * @param headers the HTTP request headers
     * @return the extracted IP address, or {@code null} if none can be determined
     */
    public String extractIp(Headers headers) {
        return this.extractIp(headers, null);
    }

    /**
     * Extracts the client IP address from the configured forwarded-IP headers,
     * using a fallback if extraction fails.
     * <p>
     * Only the first configured forwarded-IP header present in the request is used.
     *
     * @param headers  the HTTP request headers
     * @param fallback the value to return if no IP address can be extracted
     * @return the extracted IP address, or the fallback value
     */
    public String extractIp(Headers headers, String fallback) {
        String headerName = this.pickFirstForwardedIpHeader(headers.keySet());
        if (headerName == null) {
            return fallback;
        }

        String ip = this.extractIp(headers.getFirst(headerName));
        return ip != null ? ip : fallback;
    }

    /**
     * Extracts an IP address from a forwarded header value.
     * <p>
     * If the value contains multiple comma-separated entries, only the first entry
     * is considered. Surrounding whitespace is removed. Bracketed IPv6 addresses
     * are returned without their brackets. For values containing exactly one colon,
     * the suffix beginning at that colon is removed as a port.
     *
     * @param headerValue the raw forwarded header value
     * @return the extracted IP address, or {@code null} if the value is {@code null},
     * blank, or produces an empty result
     */
    public String extractIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String ip = headerValue.split(",")[0].trim();

        // IPv6
        if (ip.startsWith("[")) {
            int closingBracket = ip.indexOf(']');
            if (closingBracket >= 0) {
                return ip.substring(1, closingBracket);
            }
        }

        // IPv4
        int colonIndex = ip.lastIndexOf(':');
        if (colonIndex > 0 && ip.indexOf(':') == colonIndex) {
            ip = ip.substring(0, colonIndex);
        }

        return ip.isBlank() ? null : ip;
    }

    /**
     * Checks whether the supplied IP address is present in the configured trusted-proxy list.
     *
     * @param ip the IP address to check
     * @return {@code true} if the IP address is non-null and exactly matches a configured
     * trusted proxy; otherwise {@code false}
     */
    public boolean isTrustedProxy(String ip) {
        return ip != null && trustedProxies.contains(ip);
    }

}