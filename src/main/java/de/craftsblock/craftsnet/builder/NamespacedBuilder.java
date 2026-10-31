package de.craftsblock.craftsnet.builder;

import org.jetbrains.annotations.NotNull;

public abstract class NamespacedBuilder<T extends NamespacedBuilder<T>> extends AbstractCraftsNetBuilder<T> {

    private final CraftsNetBuilder root;
    private final String namespace;

    protected NamespacedBuilder(@NotNull CraftsNetBuilder root, @NotNull String namespace) {
        this(root, namespace, false);
    }

    protected NamespacedBuilder(@NotNull CraftsNetBuilder root, @NotNull String namespace, boolean allowUnknownArgs) {
        super(allowUnknownArgs);
        this.root = root;
        this.namespace = namespace;

        root.registerInternalNamespace(this);
    }

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

    public final String getNamespace() {
        return this.namespace;
    }

    public CraftsNetBuilder getRoot() {
        return this.root;
    }

}
