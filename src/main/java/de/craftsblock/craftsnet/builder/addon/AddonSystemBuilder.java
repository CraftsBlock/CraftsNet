package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class AddonSystemBuilder extends NamespacedBuilder<AddonSystemBuilder> {

    private final Map<String, AddonBuilder> addonBuilders;

    public AddonSystemBuilder(@NotNull CraftsNetBuilder root, AddonSystemState state) {
        super(root, "addons");
        acquire(this);
        this.addonBuilders = new HashMap<>();

        setArgTransformer("state", value -> AddonSystemState.valueOf(value.toUpperCase()));

        state(state).set("in_memory", Collections.synchronizedList(new ArrayList<>()));
    }

    @Override
    public @NotNull AddonSystemBuilder set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");

        if (!arg.contains(":")) {
            return super.set(arg, value);
        }

        String[] parts = arg.split(":", 2);
        addon(parts[0]).set(parts[1], value);
        return this;
    }

    public AddonSystemBuilder addon(@NotNull String addon, Consumer<AddonBuilder> builderConsumer) {
        builderConsumer.accept(this.addon(addon));
        return this;
    }

    public AddonBuilder addon(@NotNull String addon) {
        Objects.requireNonNull(addon, "addon");

        return this.addonBuilders.computeIfAbsent(
                addon.toLowerCase(),
                namespace -> isFrozen() ? null : new AddonBuilder(this, namespace)
        );
    }

    public @NotNull AddonSystemBuilder state(@NotNull AddonSystemState state) {
        set("state", state);
        return this;
    }

    public @NotNull AddonSystemState state() {
        return Objects.requireNonNull(
                getEnum("state", AddonSystemState.class),
                "No state for the addon system found"
        );
    }

    public boolean isState(@NotNull AddonSystemState state) {
        return this.state() == state;
    }

    @SuppressWarnings("rawtypes")
    public @NotNull Collection<Class> getInMemoryAddons() {
        return Objects
                .requireNonNull(
                        getCollection("in_memory", Class.class),
                        "No in memory addons present on the builder"
                );
    }

    public @NotNull AddonSystemBuilder add(@NotNull Class<? extends Addon> addonType) {
        getInMemoryAddons().add(addonType);
        return this;
    }

    public @NotNull AddonSystemBuilder add(@NotNull Collection<Class<? extends Addon>> addonTypes) {
        getInMemoryAddons().addAll(addonTypes);
        return this;
    }

    /**
     * Load all the addons that have been registered in the builder.
     *
     * @param craftsNet The instance of CraftsNet that the addons should be registered on.
     */
    @ApiStatus.Internal
    public void loadAddons(CraftsNet craftsNet) {
        AddonManager manager = craftsNet.getAddonManager();
        List<AddonConfiguration> configurations = new ArrayList<>();

        AddonLoader loader = manager.getAddonLoader();
        for (Class<?> type : getInMemoryAddons()) {
            Class<? extends Addon> addonType = type.asSubclass(Addon.class);
            configurations.addAll(AddonConfiguration.of(craftsNet, loader, addonType));
        }

        manager.addDirectly(new TreeSet<>(configurations));
    }

}
