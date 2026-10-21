package de.craftsblock.craftsnet.builder.addon;

public enum AddonSystemState {

    FULL,
    IN_MEMORY_ONLY,
    FILE_ONLY,
    DISABLED;

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
