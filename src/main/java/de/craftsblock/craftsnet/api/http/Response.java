package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.http.cookies.Cookie;
import de.craftsblock.craftsnet.api.http.cookies.SameSite;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The Response class represents an HTTP response sent by the web server to the client.
 * It provides methods to set response headers, status code, and send response content.
 * This class implements the AutoCloseable interface to ensure proper handling and closing of the response stream.
 * It is used internally by the WebServer to construct and send HTTP responses to client requests.
 * Developers should not create instances of this class directly but use the provided methods in the WebServer class.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see Exchange
 * @see WebServer
 * @since CraftsNet-1.0.0
 */
public class Response implements AutoCloseable {

    private final CraftsNet craftsNet;
    private final Logger logger;

    private final HttpExchange exchange;
    private final OutputStream stream;
    private final Headers headers;
    private final ConcurrentHashMap<String, Cookie> cookies = new ConcurrentHashMap<>();
    private final boolean bodyAble;

    private int code = 200;
    private boolean bodySend = false;

    /**
     * Constructor for creating a new Response object.
     *
     * @param craftsNet The CraftsNet instance which instantiates this
     * @param exchange  The HttpExchange object representing the HTTP request-response exchange.
     */
    protected Response(CraftsNet craftsNet, HttpExchange exchange) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.logger();

        this.exchange = exchange;
        this.stream = exchange.getResponseBody();
        this.headers = exchange.getResponseHeaders();

        HttpMethod method = HttpMethod.parse(exchange.getRequestMethod());
        this.bodyAble = !method.equals(HttpMethod.HEAD) && !method.equals(HttpMethod.UNKNOWN);

        setContentType("application/json");
    }

    /**
     * Sends the string representation of the provided object as the response body.
     *
     * @param object The object to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(Object object) throws IOException {
        if (!bodySend && !hasHeader("Content-Type")) setContentType("application/json");
        println(object.toString());
    }

    /**
     * Sends the provided text as the response body followed by a new line in UTF-8 encoding.
     *
     * @param text The text to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void println(String text) throws IOException {
        print(text + (text.trim().endsWith("\r\n") ? "" : "\r\n"));
    }

    /**
     * Sends the provided text as the response body in UTF-8 encoding.
     *
     * @param text The text to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(String text) throws IOException {
        print(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends the provided file as the response body.
     *
     * @param file The file to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(File file) throws IOException {
        if (bodySend) {
            logger.warning("A file was attempted to be sent while the text body has already begun to be written!");
            return;
        }

        bodySend = true;
        sendResponseHeaders(code, file.length());
        try (FileInputStream input = new FileInputStream(file)) {
            print(input);
        }
    }

    /**
     * Sends the provided bytes as the response body.
     *
     * @param bytes The bytes to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(byte[] bytes) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            print(input);
        }
    }

    /**
     * Sends the content of the provided input stream as the response body.
     *
     * @param stream The input stream which content should be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(InputStream stream) throws IOException {
        byte[] buffer = new byte[2048];
        int read;
        while ((read = stream.read(buffer)) != -1) printRaw(buffer, 0, read);
        Arrays.fill(buffer, (byte) 0);
    }

    /**
     * Sends the provided bytes as the response body.
     *
     * @param bytes The bytes to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    private void printRaw(byte[] bytes, int offset, int length) throws IOException {
        if (!bodyAble)
            throw new IllegalStateException("Body is not printable as the request method cannot have a response body!");

        if (!bodySend) {
            sendResponseHeaders(code, 0);
            bodySend = true;
        }

        stream.write(bytes, offset, length);
        stream.flush();
    }

    /**
     * Sends the response headers to the client, including any cookies that have been set.
     *
     * @param code   The HTTP status code
     * @param length The length of the response body
     * @throws IOException if an I/O error occurs
     */
    private void sendResponseHeaders(int code, long length) throws IOException {
        for (Cookie cookie : getCookies())
            addHeader("Set-Cookie", cookie.toString());

        exchange.sendResponseHeaders(code, length);
    }

    /**
     * Closes the response
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (!bodySend)
            sendResponseHeaders(code, -1);
        exchange.close();
    }

    /**
     * Sets the content type for the response.
     *
     * @param contentType The content type to be set in the response header.
     */
    public void setContentType(@Nullable String contentType) {
        setContentType(contentType, null);
    }

    /**
     * Sets the content type for the response or use the fallback value if the contentType is null. If both, the
     * preferred content type and the fallback content type are null, then no further action will be made.
     *
     * @param contentType The content type to be set in the response header.
     * @param fallback    The fallback content type to be set in the response header if the content type was null.
     */
    public void setContentType(@Nullable String contentType, @Nullable String fallback) {
        if (contentType != null) setHeader("content-type", contentType);
        else if (fallback != null) setHeader("content-type", fallback);
    }

    /**
     * Sets the HTTP status code for the response.
     *
     * @param code The HTTP status code to be set in the response.
     * @throws IllegalStateException if the response headers have already been sent.
     */
    public void setCode(int code) {
        if (bodySend)
            throw new IllegalStateException("Antwort Header wurden bereits gesendet!");
        this.code = code;
    }

    /**
     * Checks if a response header with the specified key exists.
     *
     * @param key The key of the header to check.
     * @return true if a header with the specified key exists, false otherwise.
     */
    public boolean hasHeader(String key) {
        return headers.containsKey(key);
    }

    /**
     * Gets the value of the response header with the specified key.
     *
     * @param key The key of the header to get the value for.
     * @return The value of the header with the specified key, or null if the header is not found.
     */
    public String getHeader(@NotNull String key) {
        return headers.getFirst(key);
    }

    /**
     * Adds a response header with the specified key and value.
     *
     * @param key   The key of the header to be added.
     * @param value The value of the header to be added.
     * @throws IllegalStateException if the response headers have already been sent.
     */
    public void addHeader(String key, String value) {
        if (bodySend)
            throw new IllegalStateException("Antwort Header wurden bereits gesendet!");
        if (key == null || value == null) return;
        headers.add(key, value);
    }

    /**
     * Sets a response header with the specified key and value. If a header with the same key already exists, it will be replaced.
     *
     * @param key   The key of the header to be set.
     * @param value The value of the header to be set.
     * @throws IllegalStateException if the response headers have already been sent.
     */
    public void setHeader(String key, String value) {
        if (bodySend)
            throw new IllegalStateException("Antwort Header wurden bereits gesendet!");
        if (key == null || value == null) return;
        headers.set(key, value);
    }

    /**
     * Sets a cookie with the specified name and no value.
     *
     * @param name The name of the cookie, cannot be null
     * @return The created Cookie object
     */
    public Cookie setCookie(@NotNull String name) {
        return setCookie(name, null);
    }

    /**
     * Sets a cookie with the specified name and value.
     *
     * @param name  The name of the cookie, cannot be null
     * @param value The value of the cookie, can be null
     * @return The created Cookie object
     */
    public Cookie setCookie(@NotNull String name, @Nullable Object value) {
        return setCookie(new Cookie(name, value));
    }

    /**
     * Adds a cookie to the response, overriding any existing cookie with the same name.
     *
     * @param cookie The Cookie object to add, cannot be null
     * @return The added Cookie object
     */
    public Cookie setCookie(Cookie cookie) {
        String name = cookie.getName();
        if (!cookies.containsKey(name)) cookies.put(name, cookie);
        else cookies.get(name).override(cookie);
        return cookie;
    }

    /**
     * Marks a cookie for deletion by invoking the mark deleted method on the specific cookie.
     *
     * @param name The name of the cookie to delete
     */
    public void deleteCookie(String name) {
        cookies.computeIfAbsent(name, Cookie::new).markDeleted();
    }

    /**
     * Returns a collection of all cookies currently set for the response.
     *
     * @return A ConcurrentLinkedQueue containing all cookies
     */
    public ConcurrentLinkedQueue<Cookie> getCookies() {
        return new ConcurrentLinkedQueue<>(cookies.values());
    }

    /**
     * Retrieves the underlying HttpExchange object associated with this Response.
     *
     * @return The HttpExchange object representing the HTTP request-response exchange.
     */
    public HttpExchange unsafe() {
        return exchange;
    }

}
