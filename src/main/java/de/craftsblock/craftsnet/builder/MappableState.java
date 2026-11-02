package de.craftsblock.craftsnet.builder;

/**
 * Defines a state that can be mapped to another state of the same enum type.
 * <p>
 * Implementations of this interface must be enums and provide a mapping
 * to another constant of that enum. This allows states to define an alternative
 * representation or target state that can be used by the CraftsNet builder
 * system when resolving state mappings.
 *
 * @param <E> The enum type implementing this interface
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 4.0.0
 */
public interface MappableState<E extends Enum<E> & MappableState<E>> {

    /**
     * Returns the enum constant to which this state is mapped.
     *
     * @return The target state of the same enum type
     */
    E getMapTo();

}
