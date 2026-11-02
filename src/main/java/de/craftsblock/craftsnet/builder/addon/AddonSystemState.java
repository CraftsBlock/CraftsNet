package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.builder.MappableState;

import java.util.Objects;

/**
 * Defines the available operating states of the add-on system.
 * <p>
 * The states determine whether add-ons may be loaded from files, kept in memory,
 * or both. The enum also provides the {@link #TRUE} and {@link #FALSE} aliases,
 * which map to {@link #FULL} and {@link #DISABLED}, respectively.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see MappableState
 * @since 4.0.0
 */
public enum AddonSystemState implements MappableState<AddonSystemState> {

    /**
     * Enables add-ons from both file-based and in-memory sources.
     */
    FULL,

    /**
     * Enables add-ons that are available in memory only.
     */
    IN_MEMORY_ONLY,

    /**
     * Enables add-ons that are loaded from files only.
     */
    FILE_ONLY,

    /**
     * Disables the add-on system.
     */
    DISABLED,

    /**
     * Alias for {@link #DISABLED}.
     */
    FALSE(DISABLED),

    /**
     * Alias for {@link #FULL}.
     */
    TRUE(FULL),
    ;

    /**
     * The state this enum constant maps to, or {@code null} if it represents itself.
     */
    private final AddonSystemState mapTo;

    /**
     * Creates a state that maps to itself.
     */
    AddonSystemState() {
        this(null);
    }

    /**
     * Creates a state that maps to the specified state.
     *
     * @param mapTo the state this constant maps to, or {@code null} if it represents itself
     */
    AddonSystemState(AddonSystemState mapTo) {
        this.mapTo = mapTo;
    }

    /**
     * Returns the state represented by this enum constant.
     * <p>
     * Constants without an explicitly assigned mapping return themselves.
     *
     * @return the mapped state, or this constant if no mapping is configured
     */
    @Override
    public AddonSystemState getMapTo() {
        return Objects.requireNonNullElse(this.mapTo, this);
    }

    /**
     * Checks whether the current state allows add-ons to be loaded from files.
     *
     * @return {@code true} if this state is {@link #FULL} or {@link #FILE_ONLY};
     * otherwise {@code false}
     */
    public boolean allowsFile() {
        return this == FULL || this == FILE_ONLY;
    }

    /**
     * Checks whether the current state allows add-ons to be kept in memory.
     *
     * @return {@code true} if this state is {@link #FULL} or {@link #IN_MEMORY_ONLY};
     * otherwise {@code false}
     */
    public boolean allowsInMemory() {
        return this == FULL || this == IN_MEMORY_ONLY;
    }

    /**
     * Checks whether the add-on system is considered enabled.
     * <p>
     * Only {@link #DISABLED} returns {@code false}. This check does not resolve
     * aliases through {@link #getMapTo()}.
     *
     * @return {@code false} if this state is {@link #DISABLED}; otherwise {@code true}
     */
    public boolean isEnabled() {
        return this != DISABLED;
    }

}
