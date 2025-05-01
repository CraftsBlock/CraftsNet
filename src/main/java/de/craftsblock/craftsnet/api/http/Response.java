package de.craftsblock.craftsnet.api.http;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.http.cookies.Cookie;
import de.craftsblock.craftsnet.api.http.cors.CorsPolicy;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import de.craftsblock.craftsnet.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
 * @version 1.2.2
 * @see Exchange
 * @see WebServer
 * @since 1.0.0-SNAPSHOT
 */
public class Response implements AutoCloseable {

    private final CraftsNet craftsNet;
    private final Logger logger;

    private final HttpExchange httpExchange;
    private final Headers headers;
    private final ConcurrentHashMap<String, Cookie> cookies = new ConcurrentHashMap<>();
    private final CorsPolicy corsPolicy;
    private final boolean bodyAble;

    private StreamEncoder streamEncoder;
    private OutputStream encodedStream;
    private OutputStream rawStream;

    private Exchange exchange;

    private int code = 200;
    private boolean headersSent = false;
    private boolean sendingFile = false;

    /**
     * Constructor for creating a new Response object.
     *
     * @param craftsNet     The {@link CraftsNet} instance which instantiates this
     * @param streamEncoder The {@link StreamEncoder} that should be used to encode the response body.
     * @param httpExchange  The {@link HttpExchange} object representing the HTTP request-response exchange.
     * @param httpMethod    The {@link HttpMethod} used to access the route.
     */
    protected Response(CraftsNet craftsNet, StreamEncoder streamEncoder, HttpExchange httpExchange,
                       HttpMethod httpMethod) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.logger();

        this.httpExchange = httpExchange;
        this.streamEncoder = streamEncoder;
        this.headers = httpExchange.getResponseHeaders();
        this.bodyAble = httpMethod.isResponseBodyAble();
        this.corsPolicy = new CorsPolicy();

        setContentType("application/json");
    }

    /**
     * Sends the string representation of the provided object as the response body.
     *
     * @param object The object to be sent as the response body.
     * @throws RuntimeException if an I/O error occurs.
     */
    public synchronized void print(Object object) {
        checkOutput();

        if (exchange != null) {
            Request r = exchange.request();

            if ("pretty".equalsIgnoreCase(r.retrieveParam("format"))) {
                if (object instanceof Json json) {
                    print(json, true);
                    return;
                }

                if (object instanceof JsonElement json) {
                    print(JsonParser.parse(json), true);
                    return;
                }
            }
        }

        print(object.toString());
    }


    /**
     * Sends the string representation of the provided json object as the response body while
     * setting the pretty printing flag.
     *
     * @param json   The json object to be sent as the response body.
     * @param pretty Whether the json should be printed pretty.
     * @throws RuntimeException if an I/O error occurs
     */
    public synchronized void print(Json json, boolean pretty) {
        checkOutput();
        this.print(json.toString(pretty));
    }

    /**
     * Sends the provided text as the response body followed by a new line in UTF-8 encoding.
     *
     * @param text The text to be sent as the response body.
     * @throws RuntimeException if an I/O error occurs
     */
    public synchronized void println(String text) {
        checkOutput();
        this.print(text + (text.trim().endsWith("\r\n") ? "" : "\r\n"));
    }

    /**
     * Sends the provided text as the response body in UTF-8 encoding.
     *
     * @param text The text to be sent as the response body.
     * @throws RuntimeException if an I/O error occurs
     */
    public synchronized void print(String text) {
        checkOutput();
        this.print(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends the provided file as the response body.
     *
     * @param file The file to be sent as the response body.
     * @throws RuntimeException if an I/O error occurs
     */
    public synchronized void print(File file) {
        this.print(file.toPath());
    }

    /**
     * Sends the provided file behind the {@link Path} as the response body.
     *
     * @param path The {@link Path} of the file to be sent.
     * @throws RuntimeException if an I/O error occurs
     * @since 3.3.5-SNAPSHOT
     */
    public synchronized void print(Path path) {
        checkOutput();
        if (this.headersSent)
            throw new IllegalStateException("A file was attempted to be sent while the body has already begun to be written!");

        if (Files.notExists(path))
            throw new IllegalArgumentException("The file behind the path must exist!");

        try (InputStream fileInput = Files.newInputStream(path)) {
            if (this.streamEncoder == null || this.streamEncoder.getEncodingName().equalsIgnoreCase("identity")) {
                this.ensureHeadersSend(Files.size(path));
                this.print(fileInput);
                return;
            }

            Path encodedFileLocation = craftsNet.fileHelper().createTempFile("response", ".body");
            try {
                try (OutputStream output = streamEncoder.encodeOutputStream(Files.newOutputStream(encodedFileLocation))) {
                    IOUtils.copy(fileInput, output, 2048);
                }

                try (InputStream input = Files.newInputStream(encodedFileLocation, StandardOpenOption.READ)) {
                    long size = Files.size(encodedFileLocation);
                    ensureHeadersSend(size);

                    IOUtils.copy(input, this.rawStream, Math.min((int) size, 2048));
                }
            } finally {
                Files.deleteIfExists(encodedFileLocation);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            this.sendingFile = true;
        }
    }

    /**
     * Sends the provided bytes as the response body.
     *
     * @param bytes The bytes to be sent as the response body.
     * @throws RuntimeException if an I/O error occurs
     */
    public synchronized void print(byte[] bytes) {
        checkOutput();
        this.printRaw(bytes, 0, bytes.length);
    }

    /**
     * Sends the content of the provided input stream as the response body.
     *
     * @param stream The input stream which content should be sent as the response body.
     * @throws RuntimeException if an I/O error occurs
     */
    public synchronized void print(InputStream stream) {
        checkOutput();

        byte[] buffer = new byte[2048];
        try {
            int read;
            while ((read = stream.read(buffer)) != -1)
                this.printRaw(buffer, 0, read);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Arrays.fill(buffer, (byte) 0);
        }
    }

    /**
     * Sends the provided bytes as the response body.
     *
     * @param bytes The bytes to be sent as the response body.
     * @throws RuntimeException if an I/O error occurs
     */
    private synchronized void printRaw(byte[] bytes, int offset, int length) {
        if (!bodyAble)
            throw new IllegalStateException("Body is not printable as the request method cannot have a response body!");

        try {
            ensureHeadersSend(0);

            if (this.encodedStream == null)
                this.encodedStream = this.streamEncoder != null ? this.streamEncoder.encodeOutputStream(this.rawStream) : this.rawStream;

            this.encodedStream.write(bytes, offset, length);
            this.encodedStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the output stream is still valid and can therefore be written to.
     * This method also enforces some other output blocking logic.
     *
     * @since 3.3.3-SNAPSHOT
     */
    private void checkOutput() {
        if (this.sendingFile)
            throw new IllegalStateException("A file has been written to the response, no further output can be written!");
    }

    /**
     * Ensures that the headers were send
     *
     * @throws IOException if an I/O error occurs.
     */
    private void ensureHeadersSend(long length) throws IOException {
        if (headersSent) return;
        sendResponseHeaders(code, length);
    }

    /**
     * Sends the response headers to the client, including any cookies that have been set.
     *
     * @param code   The HTTP status code
     * @param length The length of the response body
     * @throws IOException if an I/O error occurs
     */
    private void sendResponseHeaders(int code, long length) throws IOException {
        if (headersSent) return;

        for (Cookie cookie : getCookies())
            addHeader("Set-Cookie", cookie.toString());

        if (exchange != null) this.corsPolicy.apply(exchange);
        if (this.streamEncoder != null)
            this.setHeader("Content-Encoding", this.streamEncoder.getEncodingName());
        if (length == 0) this.setHeader("Transfer-Encoding", "chunked");

        httpExchange.sendResponseHeaders(code, length);
        this.headersSent = true;

        if (length == -1) return;
        this.rawStream = httpExchange.getResponseBody();
    }

    /**
     * Closes the response
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        ensureHeadersSend(-1);

        if (this.encodedStream != null && !sendingFile) {
            this.encodedStream.flush();
            this.encodedStream.close();
        }
    }

    /**
     * Sets the {@link Exchange} managing this response.
     *
     * @param exchange The {@link Exchange} managing the response
     */
    protected void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Gets the {@link Exchange} managing this response.
     *
     * @return The {@link Exchange} managing this response.
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Sets the {@link StreamEncoder} by its name that is used to encode the response body.
     *
     * @param encodingName The name of the {@link StreamEncoder} that should be used.
     * @throws IllegalStateException If the response headers have already been sent.
     * @since 3.3.5-SNAPSHOT
     */
    public void setStreamEncoder(String encodingName) {
        if (!this.craftsNet.streamEncoderRegistry().isAvailable(encodingName))
            throw new IllegalStateException("No available stream encoder found for name " + encodingName + "!");

        this.setStreamEncoder(Objects.requireNonNull(this.craftsNet.streamEncoderRegistry().retrieveEncoder(encodingName)));
    }

    /**
     * Sets the {@link StreamEncoder} that is used to encode the response body.
     *
     * @param streamEncoder The {@link StreamEncoder} that should be used.
     * @throws IllegalStateException If the response headers have already been sent.
     * @since 3.3.5-SNAPSHOT
     */
    public void setStreamEncoder(@NotNull StreamEncoder streamEncoder) {
        if (headersSent())
            throw new IllegalStateException("Response headers have already been sent!");

        this.streamEncoder = streamEncoder;
    }

    /**
     * Retrieves the {@link StreamEncoder} that is used to encode the response body.
     *
     * @return The {@link StreamEncoder} is used.
     * @since 3.3.3-SNAPSHOT
     */
    public StreamEncoder getStreamEncoder() {
        return streamEncoder;
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
        if (headersSent)
            throw new IllegalStateException("Response headers have already been sent!");
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
        if (headersSent)
            throw new IllegalStateException("Response headers have already been sent!");
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
        if (headersSent)
            throw new IllegalStateException("Response headers have already been sent!");
        if (key == null || value == null) return;
        headers.set(key, value);
    }

    /**
     * Get all the values from the response headers for the specified header name.
     *
     * @param key The "name" of the header used to find the header
     * @return A list of alle the values
     * @since 3.3.6-SNAPSHOT
     */
    public List<String> getHeaders(String key) {
        return getHeaders().get(key);
    }

    /**
     * Allows raw access to the underlying {@link Headers} object, which stores the response headers.
     *
     * @return The {@link Headers} object storing the response headers.
     * @since 3.3.6-SNAPSHOT
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Updates the cors policy added to the header of the response.
     *
     * @param corsPolicy The cors policy which should be sent.
     */
    public void setCorsPolicy(CorsPolicy corsPolicy) {
        this.corsPolicy.update(corsPolicy);
    }

    /**
     * Returns the cors policy added to the header of the response.
     *
     * @return The modifiable cors policy.
     */
    public CorsPolicy getCorsPolicy() {
        return corsPolicy;
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
        if (cookies.containsKey(name)) cookies.get(name).override(cookie);
        else cookies.put(name, cookie);
        return cookie;
    }

    /**
     * Marks a cookie for deletion by invoking the mark deleted method on the specific cookie.
     *
     * @param name The name of the cookie to delete
     * @return The deleted Cookie object
     */
    public Cookie deleteCookie(String name) {
        return cookies.computeIfAbsent(name, n -> exchange.request().retrieveCookie(n, new Cookie(n))).markDeleted();
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
     * Returns whether the response headers have already been sent or not.
     *
     * @return {@code true} if the response headers have been sent, {@code false} otherwise.
     */
    public boolean headersSent() {
        return headersSent;
    }

    /**
     * Returns whether the response was used to send a file to the client.
     *
     * @return {@code true} if a file was sent, {@code false} otherwise.
     * @since 3.3.3-SNAPSHOT
     */
    public boolean sendingFile() {
        return sendingFile;
    }

    /**
     * Returns whether the response can have a response body.
     *
     * @return {@code true} if the response can have a body, {@code false} otherwise.
     */
    public boolean isBodyAble() {
        return bodyAble;
    }

    /**
     * Retrieves the underlying HttpExchange object associated with this Response.
     *
     * @return The HttpExchange object representing the HTTP request-response exchange.
     */
    public HttpExchange unsafe() {
        return httpExchange;
    }

    /**
     * Gets the instance of CraftsNet which will send the response.
     *
     * @return The CraftsNet instance.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

}
