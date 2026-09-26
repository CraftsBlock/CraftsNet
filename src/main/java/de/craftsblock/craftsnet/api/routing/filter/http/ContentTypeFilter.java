package de.craftsblock.craftsnet.api.routing.filter.http;

import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.routing.filter.FilterChain;
import de.craftsblock.craftsnet.api.routing.filter.ValueFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public final class ContentTypeFilter extends ValueFilter<HttpExchange, HashSet<String>> {

    private ContentTypeFilter(@NotNull List<String> value) {
        super(new HashSet<>(value));
    }

    @Override
    public @Nullable Object filter(@NotNull HttpExchange exchange, @NotNull FilterChain<HttpExchange> chain) {
        final Request request = exchange.request();
        if (getValue().contains(request.getContentType())) {
            return chain.nextFilter(exchange);
        }

        return null;
    }

    public @NotNull ContentTypeFilter of(@NotNull String @NotNull ... contentTypes) {
        return new ContentTypeFilter(List.of(contentTypes));
    }

    public @NotNull ContentTypeFilter of(@NotNull List<@NotNull String> contentTypes) {
        return new ContentTypeFilter(contentTypes);
    }

}
