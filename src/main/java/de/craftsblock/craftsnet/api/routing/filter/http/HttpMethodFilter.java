package de.craftsblock.craftsnet.api.routing.filter.http;

import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.HttpMethod;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.routing.filter.Filter;
import de.craftsblock.craftsnet.api.routing.filter.FilterChain;
import de.craftsblock.craftsnet.api.routing.filter.ValueFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public final class HttpMethodFilter extends ValueFilter<HttpExchange, HashSet<HttpMethod>> {

    private HttpMethodFilter(@NotNull List<HttpMethod> value) {
        super(new HashSet<>(HttpMethod.normalize(value)), true);
    }

    @Override
    public @Nullable Object filter(@NotNull HttpExchange exchange, @NotNull FilterChain<HttpExchange> chain) {
        final Request request = exchange.request();
        if (getValue().contains(request.getHttpMethod())) {
            return chain.nextFilter(exchange);
        }

        return null;
    }

    @Override
    protected boolean overlaps(Filter<?> that) {
        if (!(that instanceof HttpMethodFilter thatFilter)) {
            return false;
        }

        return this.getValue().stream().anyMatch((thatFilter.getValue()::contains));
    }

    @Override
    public boolean equals(ValueFilter<?, ?> rawThat) {
        HttpMethodFilter that = (HttpMethodFilter) rawThat;
        return getValue().stream().anyMatch(that.getValue()::contains);
    }

    public static HttpMethodFilter of(@NotNull HttpMethod @NotNull ... methods) {
        return new HttpMethodFilter(List.of(methods));
    }

    public static HttpMethodFilter of(@NotNull List<HttpMethod> methods) {
        return new HttpMethodFilter(methods);
    }

    public static HttpMethodFilter all() {
        return of(HttpMethod.ALL.getMethods());
    }

    public static HttpMethodFilter allRaw() {
        return of(HttpMethod.ALL_RAW.getMethods());
    }

    public static HttpMethodFilter get() {
        return of(HttpMethod.GET);
    }

    public static HttpMethodFilter post() {
        return of(HttpMethod.POST);
    }

    public static HttpMethodFilter put() {
        return of(HttpMethod.PUT);
    }

    public static HttpMethodFilter delete() {
        return of(HttpMethod.DELETE);
    }

    public static HttpMethodFilter patch() {
        return of(HttpMethod.PATCH);
    }

    public static HttpMethodFilter options() {
        return of(HttpMethod.OPTIONS);
    }

    public static HttpMethodFilter head() {
        return of(HttpMethod.HEAD);
    }

}
