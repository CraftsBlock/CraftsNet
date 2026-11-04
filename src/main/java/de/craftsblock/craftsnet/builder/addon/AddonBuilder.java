package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Builder for configuring an individual add-on.
 * <p>
 * Each instance is associated with an {@link AddonSystemBuilder} and uses its
 * namespace to identify the add-on's configuration. Unlike the general namespaced
 * builder, this builder also handles arguments that may already have been transformed,
 * preventing the same argument from being transformed more than once.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see AddonSystemBuilder
 * @see NamespacedBuilder
 * @since 4.0.0
 */
public class AddonBuilder extends NamespacedBuilder<AddonBuilder> {

    /**
     * Creates a builder for the specified add-on namespace.
     * <p>
     * The builder uses the root builder of the supplied add-on system builder and
     * enables the additional namespaced configuration behavior provided by the
     * corresponding superclass constructor. It also registers itself with its
     * parent configuration.
     *
     * @param parent    the add-on system builder this builder belongs to
     * @param namespace the namespace identifying the add-on
     * @throws NullPointerException if {@code parent} is {@code null}
     */
    public AddonBuilder(@NotNull AddonSystemBuilder parent, String namespace) {
        super(parent.getRoot(), namespace, false, true);
        this.acquire(this);
    }

    /**
     * Registers a known argument and transforms its current value if necessary.
     * <p>
     * If the argument does not have a value, no transformation is performed.
     * If its current value is a {@link String}, the supplied transformer is applied
     * and the transformed value is stored. If the value is already non-string,
     * an exception is thrown to prevent transforming it again.
     *
     * @param arg         the name of the argument to register
     * @param transformer the function used to transform the argument's string value
     * @throws NullPointerException     if {@code arg} or {@code transformer} is {@code null}
     * @throws IllegalArgumentException if the argument already contains a non-string value
     */
    @Override
    public void addKnownArgument(@NotNull String arg, @NotNull Function<String, Object> transformer) {
        Object value = this.get(arg);
        if (value == null) {
            return;
        }

        if (value instanceof String string) {
            this.set(arg, transformer.apply(string));
            return;
        }

        throw new IllegalArgumentException("The addon argument " + arg + " of "
                + getNamespace() + " has already been transformed!");
    }

    /**
     * Sets the specified argument using the default value behavior of the superclass.
     *
     * @param arg the name of the argument to set
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code arg} is {@code null}
     */
    @Override
    public @NotNull AddonBuilder set(@NotNull String arg) {
        return super.set(arg);
    }

    /**
     * Sets the specified argument to the supplied value using the superclass
     * implementation.
     *
     * @param arg   the name of the argument to set
     * @param value the value to assign to the argument
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code arg} or {@code value} is {@code null}
     */
    @Override
    public @NotNull AddonBuilder set(@NotNull String arg, @NotNull Object value) {
        return super.set(arg, value);
    }

}
