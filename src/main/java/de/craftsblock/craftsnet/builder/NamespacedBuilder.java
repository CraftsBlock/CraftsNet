package de.craftsblock.craftsnet.builder;

import org.jetbrains.annotations.NotNull;

/**
 * Base implementation for builders that belong to a specific namespace
 * within the CraftsNet builder system.
 * <p>
 * A namespaced builder extends {@link AbstractCraftsNetBuilder} and associates
 * its arguments with a unique namespace. This allows multiple specialized
 * builders to contribute arguments to a common root builder while keeping
 * their respective configurations logically separated.
 * <p>
 * During construction, the builder registers itself with its root
 * {@link CraftsNetBuilder} through its internal namespace registration
 * mechanism. Argument assignment is delegated to the parent implementation,
 * with any occurring {@link Throwable} wrapped in a {@link RuntimeException}
 * that provides additional namespace context.
 *
 * @param <T> The concrete namespaced builder type
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see AbstractCraftsNetBuilder
 * @see CraftsNetBuilder
 * @since 4.0.0
 */
public abstract class NamespacedBuilder<T extends NamespacedBuilder<T>> extends AbstractCraftsNetBuilder<T> {

    private final CraftsNetBuilder root;
    private final String namespace;

    /**
     * Creates a new namespaced builder that does not allow unknown arguments.
     * <p>
     * The builder is automatically registered with the specified root builder.
     *
     * @param root      The root builder this namespace belongs to
     * @param namespace The identifier of this builder's namespace
     */
    protected NamespacedBuilder(@NotNull CraftsNetBuilder root, @NotNull String namespace) {
        this(root, namespace, false);
    }

    /**
     * Creates a new namespaced builder with the specified unknown-argument
     * handling policy.
     * <p>
     * The supplied configuration is passed to the parent implementation.
     * After initialization, this builder registers itself with the root
     * builder so that its namespace can be managed centrally.
     *
     * @param root             The root builder this namespace belongs to
     * @param namespace        The identifier of this builder's namespace
     * @param allowUnknownArgs {@code true} to allow arguments that have not
     *                         been explicitly registered, {@code false} otherwise
     */
    protected NamespacedBuilder(@NotNull CraftsNetBuilder root, @NotNull String namespace, boolean allowUnknownArgs) {
        super(allowUnknownArgs);
        this.root = root;
        this.namespace = namespace;

        root.registerInternalNamespace(this);
    }

    /**
     * Assigns a value to an argument within this namespace.
     * <p>
     * The actual argument assignment and transformation are handled by
     * {@link AbstractCraftsNetBuilder#set(String, Object)}. Any {@link Throwable}
     * occurring during this operation is wrapped in a {@link RuntimeException}
     * containing the argument name and namespace to provide additional
     * context when diagnosing configuration errors.
     *
     * @param arg   The name of the argument to assign
     * @param value The value to associate with the argument
     * @return The root builder for fluent method chaining
     * @throws RuntimeException If any {@link Throwable} occurs while assigning
     *                          the argument
     */
    @Override
    protected @NotNull T set(@NotNull String arg, @NotNull Object value) {
        try {
            return super.set(arg, value);
        } catch (Throwable e) {
            throw new RuntimeException(
                    "Failed to set argument " + arg + " for namespace " + this.namespace,
                    e
            );
        }
    }

    /**
     * Returns the namespace identifier associated with this builder.
     *
     * @return The namespace identifier
     */
    public final String getNamespace() {
        return this.namespace;
    }

    /**
     * Returns the root builder to which this namespaced builder belongs.
     *
     * @return The associated {@link CraftsNetBuilder} instance
     */
    public CraftsNetBuilder getRoot() {
        return this.root;
    }

}
