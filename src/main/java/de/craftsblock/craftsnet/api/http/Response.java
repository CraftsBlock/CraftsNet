package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The Response class represents an HTTP response sent by the web server to the client.
 * It provides methods to set response headers, status code, and send response content.
 * This class implements the AutoCloseable interface to ensure proper handling and closing of the response stream.
 * It is used internally by the WebServer to construct and send HTTP responses to client requests.
 * Developers should not create instances of this class directly but use the provided methods in the WebServer class.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
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
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            byte[] buffer = new byte[2048];
            int read;
            while ((read = input.read(buffer)) != -1) stream.write(buffer, 0, read);
            stream.flush();
            Arrays.fill(buffer, (byte) 0);
        }
    }

    /**
     * Sends the provided file as the response body.
     *
     * @param file The file to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(File file) throws IOException {
        if (bodySend) {
            logger.warning("Tried to send file when the body was already send!");
            return;
        }
        bodySend = true;
        exchange.sendResponseHeaders(code, file.length());
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[2048];
            int read;
            while ((read = input.read(buffer)) != -1) stream.write(buffer, 0, read);
            Arrays.fill(buffer, (byte) 0);
        }
    }

    /**
     * Sends the provided json object as the response body.
     *
     * @param json The json object to be sent as the response body.
     * @throws IOException if an I/O error occurs.
     */
    public void print(Json json) throws IOException {
        if (bodySend) {
            logger.warning("Tried to send file when the body was already send!");
            return;
        }

        byte[] rawData = json.toString().getBytes(StandardCharsets.UTF_8);
        setContentType("application/json");
        exchange.sendResponseHeaders(code, rawData.length);
        bodySend = true;
        print(rawData);
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
     * Retrieves the underlying HttpExchange object associated with this Response.
     *
     * @return The HttpExchange object representing the HTTP request-response exchange.
     */
    public HttpExchange unsafe() {
        return exchange;
    }

}
