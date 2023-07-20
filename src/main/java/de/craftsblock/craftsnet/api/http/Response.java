package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The Response class represents an HTTP response sent by the web server to the client.
 * It provides methods to set response headers, status code, and send response content.
 * This class implements the AutoCloseable interface to ensure proper handling and closing of the response stream.
 * It is used internally by the WebServer to construct and send HTTP responses to client requests.
 * Developers should not create instances of this class directly but use the provided methods in the WebServer class.
 *
 * @author CraftsBlock
 * @see Exchange
 * @see WebServer
 * @since 1.0.0
 */
public class Response implements AutoCloseable {

    private final HttpExchange exchange;
    private final OutputStream stream;
    private final Headers headers;

    private int code = 200;
    private boolean bodySend = false;

    /**
     * Constructor for creating a new Response object.
     *
     * @param exchange The HttpExchange object representing the HTTP request-response exchange.
     */
    protected Response(HttpExchange exchange) {
        this.exchange = exchange;
        stream = exchange.getResponseBody();
        headers = exchange.getResponseHeaders();
        setContentType("application/json");
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
     * Sends the provided text as the response body followed by a new line in UTF-8 encoding.
     *
     * @param text The text to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void println(String text) throws IOException {
        print(text + (text.trim().endsWith("\r\n") ? "" : "\r\n"));
    }

    /**
     * Sends the provided bytes as the response body.
     *
     * @param bytes The bytes to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(byte[] bytes) throws IOException {
        if (!bodySend)
            exchange.sendResponseHeaders(code, 0);
        bodySend = true;
        stream.write(bytes);
        stream.flush();
    }

    /**
     * Closes the response
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (!bodySend)
            exchange.sendResponseHeaders(code, 0);
        stream.close();
    }

    /**
     * Sets the content type for the response.
     *
     * @param contentType The content type to be set in the response header.
     */
    public void setContentType(String contentType) {
        setHeader("content-type", contentType);
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
    public String getHeader(String key) {
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
        headers.set(key, value);
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
