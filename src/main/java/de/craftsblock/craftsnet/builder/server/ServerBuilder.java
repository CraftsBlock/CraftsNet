package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.AbstractCraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

public class ServerBuilder extends AbstractCraftsNetBuilder<ServerBuilder> {

    public ServerBuilder(@NotNull CraftsNetBuilder parent, @NotNull ServerState state, int port, boolean skipDefaultRoute) {
        super(parent);
        acquire(this);

        state(state).port(port).skipDefaultRoute(skipDefaultRoute);
    }

    public @NotNull ServerBuilder state(@NotNull ServerState state) {
        set("state", state);
        return this;
    }

    public @NotNull ServerState state() {
        return asEnum("state", ServerState.class);
    }

    public boolean isState(@NotNull ServerState state) {
        return this.state() == state;
    }

    public @NotNull ServerBuilder port(int port) {
        set("port", port);
        return this;
    }

    public int port() {
        return asShort("port");
    }

    public @NotNull ServerBuilder skipDefaultRoute(boolean skipDefaultRoute) {
        set("skipDefaultRoute", skipDefaultRoute);
        return this;
    }

    public boolean skipDefaultRoute() {
        return asBoolean("skipDefaultRoute");
    }

    public boolean isPort(int port) {
        return this.port() == port;
    }

}
