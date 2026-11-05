package de.craftsblock.craftsnet.builder.server;

import com.sun.net.httpserver.Headers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
 * @param trustedProxies       the list of trusted proxy IP addresses or CIDR ranges
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
     * @param trustedProxies       the proxy IP addresses or CIDR ranges considered trusted
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
     * @param connectingIp the ip that was used to connect to the server
     * @param headers      the HTTP request headers
     * @return the extracted host, or {@code null} if no host can be determined
     */
    public String extractHost(String connectingIp, Headers headers) {
        return this.extractHost(connectingIp, headers, null);
    }

    /**
     * Extracts the host from the supplied HTTP headers, using a fallback if extraction fails.
     * <p>
     * The first configured forwarded-host header present in the request is preferred
     * if the connecting IP belongs to a trusted proxy.
     * If no configured forwarded-host header is present, or the connecting IP
     * is not trusted, the {@code Host} header is used when available.
     *
     * @param connectingIp the ip that was used to connect to the server
     * @param headers      the HTTP request headers
     * @param fallback     the value to return if no host can be extracted
     * @return the extracted host, or the fallback value
     */
    public String extractHost(String connectingIp, Headers headers, String fallback) {
        String headerName = this.pickFirstForwardedHostHeader(headers.keySet());

        String host;
        if (headerName != null && this.isTrustedProxy(connectingIp)) {
            host = this.extractHost(headers.getFirst(headerName));
        } else if (headers.containsKey("Host")) {
            host = this.extractHost(headers.getFirst("Host"));
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

        String host = headerValue.split(",", 2)[0].trim();

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
     * @param connectingIp the ip that was used to connect to the server
     * @param headers      the HTTP request headers
     * @return the extracted IP address, or {@code null} if none can be determined
     */
    public String extractIp(String connectingIp, Headers headers) {
        return this.extractIp(connectingIp, headers, null);
    }

    /**
     * Extracts the client IP address from the configured forwarded-IP headers,
     * using a fallback if extraction fails.
     * <p>
     * Forwarded IP headers are only considered when the connecting IP belongs
     * to a trusted proxy. The forwarded IP chain is traversed from right to left,
     * skipping trusted proxies until the first untrusted address is found.
     * Invalid entries cause extraction to fail.
     *
     * @param connectingIp the ip that was used to connect to the server
     * @param headers      the HTTP request headers
     * @param fallback     the value to return if no IP address can be extracted
     * @return the extracted IP address, or the fallback value
     */
    public String extractIp(String connectingIp, Headers headers, String fallback) {
        if (!this.isTrustedProxy(connectingIp)) {
            return fallback;
        }

        String headerName = this.pickFirstForwardedIpHeader(headers.keySet());
        if (headerName == null) {
            return fallback;
        }

        String ip = this.extractClientIp(headers.getFirst(headerName));
        return ip != null ? ip : fallback;
    }

    /**
     * Extracts an IP address from a forwarded header value.
     * <p>
     * If the value contains multiple comma-separated entries, only the first entry
     * is considered. Surrounding whitespace is removed. Bracketed IPv6 addresses
     * are returned without their brackets. IPv4 addresses with ports are also supported.
     * The resulting address must be a valid numeric IP address.
     *
     * @param headerValue the raw forwarded header value
     * @return the extracted IP address, or {@code null} if the value is {@code null},
     * blank, or produces an invalid IP address
     */
    public String extractIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String ip = headerValue.split(",", 2)[0].trim();

        InetAddress address = this.parseForwardedIp(ip);
        return address != null ? address.getHostAddress() : null;
    }

    /**
     * Traverses a forwarded IP chain from right to left and returns
     * the first address that does not belong to a trusted proxy.
     *
     * @param headerValue the raw forwarded IP chain
     * @return the first untrusted IP address, or {@code null} if extraction fails
     */
    private String extractClientIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String[] chain = headerValue.split(",");
        InetAddress[] addresses = new InetAddress[chain.length];

        // Validate the entire chain before trusting any entry
        for (int i = 0; i < chain.length; i++) {
            addresses[i] = this.parseForwardedIp(chain[i].trim());

            if (addresses[i] == null) {
                return null;
            }
        }

        // The rightmost address is the nearest forwarded hop
        for (int i = addresses.length - 1; i >= 0; i--) {
            InetAddress address = addresses[i];

            if (!this.isTrustedProxy(address)) {
                return address.getHostAddress();
            }
        }

        return null;
    }

    /**
     * Parses a numeric IP address from a forwarded header entry.
     * <p>
     * Supports IPv4 addresses, IPv4 addresses with ports, bare IPv6 addresses,
     * and bracketed IPv6 addresses with optional ports.
     * Hostnames and DNS resolution are not permitted.
     *
     * @param value the forwarded IP entry
     * @return the parsed IP address, or {@code null} if invalid
     */
    private InetAddress parseForwardedIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String ip = value.trim();

        // Bracketed IPv6
        if (ip.startsWith("[")) {
            int closingBracket = ip.indexOf(']');

            if (closingBracket < 0) {
                return null;
            }

            String suffix = ip.substring(closingBracket + 1);

            if (!suffix.isEmpty()
                    && (!suffix.startsWith(":") || !this.isValidPort(suffix.substring(1)))) {
                return null;
            }

            ip = ip.substring(1, closingBracket);

            if (!ip.contains(":")) {
                return null;
            }

            return this.parseNumericIp(ip);
        }

        // IPv4 with port
        int colonIndex = ip.lastIndexOf(':');

        if (colonIndex > 0 && ip.indexOf(':') == colonIndex && ip.contains(".")) {
            String port = ip.substring(colonIndex + 1);

            if (!this.isValidPort(port)) {
                return null;
            }

            ip = ip.substring(0, colonIndex);
        }

        return this.parseNumericIp(ip);
    }

    /**
     * Checks whether a string represents a valid numeric port.
     *
     * @param port the port value
     * @return {@code true} if the port is between 0 and 65535
     */
    private boolean isValidPort(String port) {
        if (port == null || !port.matches("\\d{1,5}")) {
            return false;
        }

        try {
            return Integer.parseInt(port) <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Parses a numeric IPv4 or IPv6 address without performing DNS resolution.
     *
     * @param ip the numeric IP address
     * @return the parsed address, or {@code null} if invalid
     */
    private InetAddress parseNumericIp(String ip) {
        if (ip == null || ip.isBlank() || ip.contains("%")) {
            return null;
        }

        // IPv4
        if (ip.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$")) {
            try {
                byte[] address = new byte[4];
                String[] parts = ip.split("\\.");

                for (int i = 0; i < parts.length; i++) {
                    int octet = Integer.parseInt(parts[i]);

                    if (octet > 255) {
                        return null;
                    }

                    address[i] = (byte) octet;
                }

                return InetAddress.getByAddress(address);
            } catch (UnknownHostException | NumberFormatException e) {
                return null;
            }
        }

        // IPv6
        if (ip.contains(":")) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                return address instanceof Inet6Address ? address : null;
            } catch (UnknownHostException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Checks whether the supplied IP address is present in the configured trusted-proxy list.
     * <p>
     * Both individual IP addresses and CIDR ranges are supported.
     *
     * @param ip the IP address to check
     * @return {@code true} if the IP address belongs to a configured trusted proxy;
     * otherwise {@code false}
     */
    public boolean isTrustedProxy(String ip) {
        InetAddress address = this.parseNumericIp(ip);
        return this.isTrustedProxy(address);
    }

    /**
     * Checks whether the supplied address belongs to any configured trusted proxy.
     *
     * @param address the address to check
     * @return {@code true} if the address belongs to a trusted proxy
     */
    public boolean isTrustedProxy(InetAddress address) {
        if (address == null) {
            return false;
        }

        return trustedProxies.stream()
                .anyMatch(proxy -> this.isInCidr(address, proxy));
    }

    /**
     * Checks whether an IP address belongs to a specified CIDR range.
     *
     * @param address the IP address to check
     * @param cidr    the configured IP address or CIDR range
     * @return {@code true} if the address belongs to the range
     */
    private boolean isInCidr(InetAddress address, String cidr) {
        if (cidr == null || cidr.isBlank()) {
            return false;
        }

        String[] parts = cidr.split("/");
        if (parts.length > 2) {
            return false;
        }

        InetAddress network = this.parseNumericIp(parts[0].trim());
        if (network == null) {
            return false;
        }

        byte[] addressBytes = address.getAddress();
        byte[] networkBytes = network.getAddress();

        // IPv4 and IPv6 must not be mixed
        if (addressBytes.length != networkBytes.length) {
            return false;
        }

        int maxPrefix = networkBytes.length * 8;
        int prefix = maxPrefix;
        if (parts.length == 2) {
            String prefixValue = parts[1].trim();

            if (!prefixValue.matches("\\d{1,3}")) {
                return false;
            }

            try {
                prefix = Integer.parseInt(prefixValue);
            } catch (NumberFormatException e) {
                return false;
            }

            if (prefix < 0 || prefix > maxPrefix) {
                return false;
            }
        }

        // Compare complete bytes
        int fullBytes = prefix / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (addressBytes[i] != networkBytes[i]) {
                return false;
            }
        }

        // Compare remaining bits
        int remainingBits = prefix % 8;
        if (remainingBits > 0) {
            int mask = 0xFF << (8 - remainingBits);

            return (addressBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        }

        return true;
    }

    /**
     * Validates whether the given host conforms to a valid domain name,
     * IPv4 address, or IPv6 address.
     *
     * <p>Domain names must not exceed 253 characters in total, and each label
     * must contain between 1 and 63 characters. Labels may consist of
     * alphanumeric characters and hyphens, but must not start or end with a
     * hyphen.
     *
     * <p>IPv4 and IPv6 addresses are also supported. IPv6 addresses may be
     * enclosed in square brackets.
     *
     * <p>Ports must be removed before validation.
     *
     * @param domain the domain name or IP address to validate
     * @return {@code true} if the host is valid, {@code false} otherwise
     */
    public static boolean isValidDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }

        if (isValidIp(domain)) {
            return true;
        }

        return domain.matches("^(?=.{1,253}$)[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");
    }

    /**
     * Validates whether the given string represents a valid IPv4 or IPv6 address.
     *
     * <p>IPv4 addresses are validated by checking that they consist of exactly
     * four decimal octets, each ranging from 0 to 255.
     *
     * <p>IPv6 addresses are validated using {@link InetAddress}.
     * IPv6 addresses enclosed in square brackets are also supported.
     *
     * <p>Hostnames and domain names are not accepted.
     *
     * @param ip the IP address to validate
     * @return {@code true} if the given string is a valid IPv4 or IPv6 address,
     * {@code false} otherwise
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        String value = ip.trim();

        // IPv6 brackets
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }

        if (value.contains("%")) {
            return false;
        }

        // IPv4
        if (value.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$")) {
            try {
                for (String part : value.split("\\.")) {
                    if (Integer.parseInt(part) > 255) {
                        return false;
                    }
                }

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // IPv6
        if (value.contains(":")) {
            try {
                return InetAddress.getByName(value) instanceof Inet6Address;
            } catch (UnknownHostException e) {
                // Invalid IP address
            }
        }

        return false;
    }

}
