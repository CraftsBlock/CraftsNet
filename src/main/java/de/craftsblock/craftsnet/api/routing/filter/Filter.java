package de.craftsblock.craftsnet.api.routing.filter;

import de.craftsblock.craftsnet.api.Exchange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Filter<E extends Exchange> {

    @Nullable Object filter(@NotNull E exchange, @NotNull FilterChain<E> chain);

}
