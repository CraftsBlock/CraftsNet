package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.builder.MappableState;

import java.util.Objects;

public enum AddonSystemState implements MappableState<AddonSystemState> {

    FULL,
    IN_MEMORY_ONLY,
    FILE_ONLY,
    DISABLED,

    FALSE(DISABLED),
    TRUE(FULL),
    ;

    private final AddonSystemState mapTo;

    AddonSystemState() {
        this(null);
    }

    AddonSystemState(AddonSystemState mapTo) {
        this.mapTo = mapTo;
    }

    @Override
    public AddonSystemState getMapTo() {
        return Objects.requireNonNullElse(this.mapTo, this);
    }

    public boolean allowsFile() {
        return this == FULL || this == FILE_ONLY;
    }

    public boolean allowsInMemory() {
        return this == FULL || this == IN_MEMORY_ONLY;
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }

}
