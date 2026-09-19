package de.craftsblock.craftsnet.api.routing.builder;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.routing.RouteInfo;
import de.craftsblock.craftsnet.api.routing.Router;
import org.jetbrains.annotations.NotNull;

public sealed interface RouteBuilder<E extends Exchange> permits LambdaRouteBuilder, ReflectionRouteBuilder {

    @NotNull RouteInfo<E> build();

    @NotNull Router getRouter();

}
