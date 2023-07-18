package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Response implements AutoCloseable {

    private final HttpExchange exchange;
    //    private final ByteArrayOutputStream stream;
    private final OutputStream stream;
    private final Headers headers;

    private int code = 200;
    private boolean bodySend = false;

    protected Response(HttpExchange exchange) {
        this.exchange = exchange;
        stream = exchange.getResponseBody();
        headers = exchange.getResponseHeaders();
        setContentType("application/json");
//        new BufferedOutputStream(exchange.getResponseBody());
    }

    public void print(String text) throws IOException {
        print(text.getBytes(StandardCharsets.UTF_8));
    }

    public void println(String text) throws IOException {
        print(text + (text.trim().endsWith("\r\n") ? "" : "\r\n"));
    }

    public void print(byte[] bytes) throws IOException {
        if (!bodySend)
            exchange.sendResponseHeaders(code, 0);
        bodySend = true;
        stream.write(bytes);
        stream.flush();
    }

    @Override
    public void close() throws IOException {
//        if (bodySend)
//            throw new IllegalStateException("Antwort wurde bereits vollständig übertragen!");
//        bodySend = true;
//        OutputStream responseBody = exchange.getResponseBody();
//        responseBody.write(stream.toByteArray());
        if (!bodySend)
            exchange.sendResponseHeaders(code, 0);
        stream.close();
    }

    public void setContentType(String contentType) {
        setHeader("content-type", contentType);
    }

    public void setCode(int code) {
        if (bodySend)
            throw new IllegalStateException("Antwort Header wurden bereits gesendet!");
        this.code = code;
    }

    public boolean hasHeader(String key) {
        return headers.containsKey(key);
    }

    public String getHeader(String key) {
        return headers.getFirst(key);
    }

    public void addHeader(String key, String value) {
        if (bodySend)
            throw new IllegalStateException("Antwort Header wurden bereits gesendet!");
        headers.add(key, value);
    }

    public void setHeader(String key, String value) {
        if (bodySend)
            throw new IllegalStateException("Antwort Header wurden bereits gesendet!");
        headers.set(key, value);
    }

    protected HttpExchange getExchange() {
        return exchange;
    }

}
