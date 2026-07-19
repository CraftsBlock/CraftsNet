package de.craftsblock.craftsnet.builder.server;

public enum ServerState {

    FORCE,
    DYNAMIC,
    DISABLED;

    public boolean isEnabled() {
        return this != DISABLED;
    }

}
