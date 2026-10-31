package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.MappableState;

import java.util.Objects;

public enum ServerState implements MappableState<ServerState> {

    FORCE,
    DYNAMIC,
    DISABLED,

    FALSE(DISABLED),
    TRUE(DYNAMIC),
    ;

    private final ServerState mapTo;

    ServerState() {
        this(null);
    }

    ServerState(ServerState mapTo) {
        this.mapTo = mapTo;
    }

    @Override
    public ServerState getMapTo() {
        return Objects.requireNonNullElse(this.mapTo, this);
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }

}
