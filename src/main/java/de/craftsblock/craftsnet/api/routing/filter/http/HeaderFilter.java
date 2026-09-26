package de.craftsblock.craftsnet.api.routing.filter.http;

import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.routing.filter.FilterChain;
import de.craftsblock.craftsnet.api.routing.filter.ValueFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public class HeaderFilter extends ValueFilter<HttpExchange, HashSet<String>> {

    protected HeaderFilter(@NotNull List<String> value) {
        super(new HashSet<>(value));
    }

    @Override
    public @Nullable Object filter(@NotNull HttpExchange exchange, @NotNull FilterChain<HttpExchange> chain) {
        final Request request = exchange.request();
        if (request.getHeaders().keySet().containsAll(getValue())) {
            return chain.nextFilter(exchange);
        }

        return null;
    }

    public static @NotNull HeaderFilter of(@NotNull String @NotNull ... headers) {
        return new HeaderFilter(List.of(headers));
    }

    public static @NotNull HeaderFilter of(@NotNull List<String> headers) {
        return new HeaderFilter(headers);
    }

}
