package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.AbstractCraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

public class ServerBuilder extends AbstractCraftsNetBuilder<ServerBuilder> {

    public ServerBuilder(@NotNull CraftsNetBuilder parent, @NotNull ServerState state, int port) {
        super(parent);
        acquire(this);

        state(state).port(port);
    }

    public @NotNull ServerBuilder state(@NotNull ServerState state) {
        set("state", state, Enum::name);
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

    public boolean isPort(int port) {
        return this.port() == port;
    }

}
