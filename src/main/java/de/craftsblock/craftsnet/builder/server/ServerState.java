package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.MappableState;

import java.util.Objects;

/**
 * Defines the possible states of a server configuration within the CraftsNet
 * builder system.
 * <p>
 * This enum provides three primary states:
 * <ul>
 *     <li>{@link #FORCE} - Represents a forced server state.</li>
 *     <li>{@link #DYNAMIC} - Represents a dynamically determined server state.</li>
 *     <li>{@link #DISABLED} - Represents a disabled server state.</li>
 * </ul>
 * <p>
 * Additionally, {@link #FALSE} and {@link #TRUE} provide boolean-compatible
 * aliases that map to {@link #DISABLED} and {@link #DYNAMIC}, respectively.
 * This allows boolean-like input values to be resolved into the corresponding
 * server states through the {@link MappableState} mechanism.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see MappableState
 * @since 4.0.0
 */
public enum ServerState implements MappableState<ServerState> {

    /**
     * Represents a server state that is explicitly forced.
     */
    FORCE,

    /**
     * Represents a server state that is determined dynamically.
     */
    DYNAMIC,

    /**
     * Represents a server state in which the server functionality is disabled.
     */
    DISABLED,

    /**
     * Boolean-compatible alias for {@link #DISABLED}.
     */
    FALSE(DISABLED),

    /**
     * Boolean-compatible alias for {@link #DYNAMIC}.
     */
    TRUE(DYNAMIC),
    ;

    /**
     * The server state to which this enum constant maps.
     * <p>
     * A value of {@code null} indicates that the constant maps to itself.
     */
    private final ServerState mapTo;

    /**
     * Creates a server state that maps to itself.
     * <p>
     * This constructor is used by the primary server states that do not
     * require an explicit mapping.
     */
    ServerState() {
        this(null);
    }

    /**
     * Creates a server state that maps to the specified target state.
     * <p>
     * This constructor is primarily used for boolean-compatible aliases.
     *
     * @param mapTo The target server state, or {@code null} if the state
     *              should map to itself
     */
    ServerState(ServerState mapTo) {
        this.mapTo = mapTo;
    }

    /**
     * Returns the server state to which this constant is mapped.
     * <p>
     * If no explicit mapping was defined, the current enum constant is
     * returned instead.
     *
     * @return The mapped server state, or this constant if no mapping exists
     */
    @Override
    public ServerState getMapTo() {
        return Objects.requireNonNullElse(this.mapTo, this);
    }

    /**
     * Checks whether the server state represents an enabled configuration.
     * <p>
     * Every state except {@link #DISABLED} is considered enabled.
     * This includes {@link #FORCE}, {@link #DYNAMIC} and their respective
     * boolean-compatible aliases.
     *
     * @return {@code true} if the server state is not {@link #DISABLED},
     * {@code false} otherwise
     */
    public boolean isEnabled() {
        return this != DISABLED;
    }

}
