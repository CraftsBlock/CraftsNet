package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;

public class AddonBuilder extends NamespacedBuilder<AddonBuilder> {

    public AddonBuilder(@NotNull AddonSystemBuilder parent, String namespace) {
        super(parent.getRoot(), namespace, true);
        this.acquire(this);
    }

    @Override
    public @NotNull AddonBuilder set(@NotNull String arg) {
        return super.set(arg);
    }

    @Override
    public @NotNull AddonBuilder set(@NotNull String arg, @NotNull Object value) {
        return super.set(arg, value);
    }

}
