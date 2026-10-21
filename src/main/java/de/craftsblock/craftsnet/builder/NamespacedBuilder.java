package de.craftsblock.craftsnet.builder;

import org.jetbrains.annotations.NotNull;

public class NamespacedBuilder<T extends NamespacedBuilder<T>> extends AbstractCraftsNetBuilder<T> {

    private final CraftsNetBuilder root;
    private final String namespace;

    public NamespacedBuilder(@NotNull CraftsNetBuilder root, @NotNull String namespace) {
        this.root = root;
        this.namespace = namespace;
        root.registerInternalNamespace(this);
    }

    public final String getNamespace() {
        return namespace;
    }

    public CraftsNetBuilder getRoot() {
        return root;
    }

}
