package de.craftsblock.craftsnet.builder.namespace;

import de.craftsblock.craftsnet.builder.AbstractCraftsNetBuilder;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

public class NamespacedBuilder extends AbstractCraftsNetBuilder<NamespacedBuilder> {

    private final String namespace;

    public NamespacedBuilder(@NotNull CraftsNetBuilder parent, @NotNull String namespace) {
        super(parent);
        acquire(this);

        this.namespace = namespace;
    }

    public final String getNamespace() {
        return namespace;
    }

}
