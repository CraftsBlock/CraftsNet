package de.craftsblock.craftsnet.api.routing.filter.http;

import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.routing.filter.FilterChain;
import de.craftsblock.craftsnet.api.routing.filter.ValueFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public class QueryParameterFilter extends ValueFilter<HttpExchange, HashSet<String>> {

    protected QueryParameterFilter(@NotNull List<String> value) {
        super(new HashSet<>(value));
    }

    @Override
    public @Nullable Object filter(@NotNull HttpExchange exchange, @NotNull FilterChain<HttpExchange> chain) {
        final Request request = exchange.request();
        if (request.getQueryParams().keySet().containsAll(getValue())) {
            return chain.nextFilter(exchange);
        }

        return null;
    }

    public static @NotNull QueryParameterFilter of(@NotNull String @NotNull ... names) {
        return new QueryParameterFilter(List.of(names));
    }

    public static @NotNull QueryParameterFilter of(@NotNull List<String> names) {
        return new QueryParameterFilter(names);
    }

}
