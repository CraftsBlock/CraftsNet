package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;

public class AddonBuilder extends NamespacedBuilder<AddonBuilder> {

    public AddonBuilder(@NotNull AddonSystemBuilder parent, String namespace) {
        super(parent.getRoot(), namespace);
        this.acquire(this);
    }

}
