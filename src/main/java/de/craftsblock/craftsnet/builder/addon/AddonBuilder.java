package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class AddonBuilder extends NamespacedBuilder<AddonBuilder> {

    public AddonBuilder(@NotNull AddonSystemBuilder parent, String namespace) {
        super(parent.getRoot(), namespace, true);
        this.acquire(this);
    }

    public void addKnownArgument(@NotNull String arg, @NotNull Function<String, Integer> transformer) {
        this.addKnownArgument(arg, (ArgumentTransformer) transformer::apply);
    }

    @Override
    public void addKnownArgument(@NotNull String arg, @NotNull ArgumentTransformer transformer) {
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

    @Override
    public @NotNull AddonBuilder set(@NotNull String arg) {
        return super.set(arg);
    }

    @Override
    public @NotNull AddonBuilder set(@NotNull String arg, @NotNull Object value) {
        return super.set(arg, value);
    }

}
