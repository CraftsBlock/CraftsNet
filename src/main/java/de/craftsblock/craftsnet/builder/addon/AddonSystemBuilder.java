package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.builder.AbstractCraftsNetBuilder;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AddonSystemBuilder extends AbstractCraftsNetBuilder<AddonSystemBuilder> {

    public AddonSystemBuilder(@NotNull CraftsNetBuilder parent, AddonSystemState state) {
        super(parent);
        acquire(this);

        state(state).set("in_memory", Collections.synchronizedList(new ArrayList<>()));
    }

    public @NotNull AddonSystemBuilder state(@NotNull AddonSystemState state) {
        set("state", state);
        return this;
    }

    public @NotNull AddonSystemState state() {
        return asEnum("state", AddonSystemState.class);
    }

    public boolean isState(@NotNull AddonSystemState state) {
        return this.state() == state;
    }

    public @NotNull AddonSystemBuilder add(@NotNull Class<? extends Addon> addonType) {
        asCollection("in_memory", Class.class).add(addonType);
        return this;
    }

    public @NotNull AddonSystemBuilder add(@NotNull Collection<Class<? extends Addon>> addonTypes) {
        asCollection("in_memory", Class.class).addAll(addonTypes);
        return this;
    }

    public AddonSystemBuilder map(@NotNull Class<? extends Addon> type, @NotNull String name) {
        AddonConfiguration.map(type, name);
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
        for (Class<?> type : asCollection("in_memory", Class.class)) {
            Class<? extends Addon> addonType = type.asSubclass(Addon.class);
            configurations.addAll(AddonConfiguration.of(craftsNet, loader, addonType));
        }

        manager.addDirectly(new TreeSet<>(configurations));
    }

}
