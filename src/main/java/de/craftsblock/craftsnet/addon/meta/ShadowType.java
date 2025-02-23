package de.craftsblock.craftsnet.addon.meta;

/**
 * Enum representing the types of resources in an addon context.
 * <p>
 * This enum defines the available categories of resources:
 * <ul>
 *   <li>{@link #DEPENDENCY} - Indicates that the shadow is a dependency required by the addon.</li>
 *   <li>{@link #REPOSITORY} - Indicates that the shadow is a repository reference needed for resolving dependencies.</li>
 * </ul>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.4-SNAPSHOT
 */
public enum ShadowType {

    /**
     * Represents a dependency required by an addon.
     */
    DEPENDENCY,

    /**
     * Represents a repository reference for resolving dependencies.
     */
    REPOSITORY,

}
