package de.craftsblock.craftsnet.builder;

/**
 * Enum representing activation types for various components in the CraftsNet framework.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.3
 */
@Deprecated(since = "3.7.0", forRemoval = true)
public enum ActivateType {

    /**
     * Indicates that the component is enabled.
     */
    ENABLED,

    /**
     * Indicates that the component is disabled.
     */
    DISABLED,

    /**
     * Indicates that the activation of the component is dynamic, possibly determined at runtime.
     */
    DYNAMIC

}
