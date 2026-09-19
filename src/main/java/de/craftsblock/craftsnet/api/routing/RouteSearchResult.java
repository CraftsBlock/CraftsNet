package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftsnet.api.Exchange;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record RouteSearchResult<E extends Exchange>(@NotNull RouteInfo<E> routeInfo, @NotNull Map<String, String> params) {

    public static final @NotNull Map<String, String> EMPTY_PARAMS = Collections.emptyMap();

    public boolean isParameterized() {
        return !EMPTY_PARAMS.equals(params);
    }

    public static <T extends Exchange> @NotNull RouteSearchResult<T> of(@NotNull RouteInfo<T> routeInfo) {
        return RouteSearchResult.of(routeInfo, EMPTY_PARAMS);
    }

    public static <T extends Exchange> @NotNull RouteSearchResult<T> of(@NotNull RouteInfo<T> routeInfo,
                                                                        @NotNull Map<String, String> params) {

        return new RouteSearchResult<>(routeInfo, params);
    }

}
