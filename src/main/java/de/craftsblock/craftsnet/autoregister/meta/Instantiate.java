package de.craftsblock.craftsnet.autoregister.meta;

/**
 * The {@link Instantiate} enum defines the different ways an instance of a class
 * can be managed during the auto registration process.
 * <p>
 * This enum is primarily used to determine whether a new instance should be created
 * every time the class is accessed or whether the same instance should be reused.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.2
 * @since 3.3.2-SNAPSHOT
 */
public enum Instantiate {

    /**
     * Indicates that the same instance of the class should be reused.
     */
    SAME,

    /**
     * Indicates that a new instance of the class should be created each time it is accessed.
     */
    NEW,

}
