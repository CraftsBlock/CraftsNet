package de.craftsblock.craftsnet.api.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftscore.utils.Validator;
import de.craftsblock.craftsnet.api.RouteRegistry;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Request {

    private final HttpExchange exchange;
    private final Headers headers;
    private final Json queryParams = JsonParser.parse("{}");
    private final Json cookies = JsonParser.parse("{}");
    private final String ip;

    private String body;
    private RouteRegistry.RouteMapping route;

    public Request(HttpExchange exchange, String query, String ip) {
        this.exchange = exchange;
        this.headers = exchange.getRequestHeaders();
        this.ip = ip;

        Arrays.stream(query.split("&")).forEach(pair -> {
            String[] stripped = pair.split("=");
            if (stripped.length != 2)
                return;
            queryParams.set(stripped[0], stripped[1]);
        });

        if (headers.containsKey("cookie"))
            Arrays.stream(headers.getFirst("cookie").split("; ")).forEach(pair -> {
                String[] stripped = pair.split("=");
                if (stripped.length != 2)
                    return;
                cookies.set(stripped[0], stripped[1]);
            });
    }

    public boolean hasParam(String key) {
        return queryParams.contains(key);
    }

    @Nullable
    public String retrieveParam(String key) {
        if (hasParam(key))
            return queryParams.getString(key);
        return null;
    }

    public boolean hasCookie(String key) {
        return cookies.contains(key);
    }

    @Nullable
    public String retrieveCookie(String key) {
        if (hasCookie(key))
            return cookies.getString(key);
        return null;
    }

    @Nullable
    public RouteRegistry.RouteMapping getRoute() {
        return route;
    }

    public boolean hasBody() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            return (body = reader.readLine()) != null && Validator.isJsonValid(body);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Nullable
    public Json getBody() {
        return (hasBody() ? JsonParser.parse(body) : null);
    }

    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    @Nullable
    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    public String getIp() {
        return ip;
    }

    public RequestMethod getRequestMethod() {
        return RequestMethod.parse(exchange.getRequestMethod());
    }

    protected HttpExchange getExchange() {
        return exchange;
    }

    protected void setRoute(RouteRegistry.RouteMapping route) {
        this.route = route;
    }

}
