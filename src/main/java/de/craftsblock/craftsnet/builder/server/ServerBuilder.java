package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.AbstractCraftsNetBuilder;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ServerBuilder extends NamespacedBuilder<ServerBuilder> {

    public ServerBuilder(@NotNull CraftsNetBuilder root, String namespace, @NotNull ServerState state, short port, boolean ssl, boolean skipDefaultRoute) {
        super(root, namespace);
        acquire(this);

        this.setArgTransformer("state", value -> AbstractCraftsNetBuilder.enumArgTransformer(ServerState.class));
        this.setArgTransformer("port", Short::parseShort);
        this.setArgTransformer("ssl", value -> AbstractCraftsNetBuilder.flagArgTransformer());
        this.setArgTransformer("skipDefaultRoute", AbstractCraftsNetBuilder.flagArgTransformer());

        state(state).port(port).ssl(ssl).skipDefaultRoute(skipDefaultRoute);
    }

    public @NotNull ServerBuilder state(@NotNull ServerState state) {
        set("state", state);
        return this;
    }

    public @NotNull ServerState state() {
        return Objects.requireNonNull(
                getEnum("state", ServerState.class),
                "No state for the addon system found"
        );
    }

    public boolean isState(@NotNull ServerState state) {
        return this.state() == state;
    }

    public @NotNull ServerBuilder port(short port) {
        set("port", port);
        return this;
    }

    public short port() {
        return Objects.requireNonNull(
                getShort("port"),
                "No port found for the " + getNamespace()
        );
    }

    public @NotNull ServerBuilder ssl(boolean ssl) {
        set("ssl", ssl);
        return this;
    }

    public boolean ssl() {
        return Objects.requireNonNull(
                getBoolean("ssl"),
                "No ssl state found for the " + getNamespace()
        );
    }

    public @NotNull ServerBuilder skipDefaultRoute(boolean skipDefaultRoute) {
        set("skipDefaultRoute", skipDefaultRoute);
        return this;
    }

    public boolean skipDefaultRoute() {
        return Objects.requireNonNull(
                getBoolean("skipDefaultRoute"),
                "No skip default route state found for the " + getNamespace()
        );
    }

    public boolean isPort(short port) {
        return this.port() == port;
    }

}
