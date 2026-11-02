package de.craftsblock.craftsnet.builder.addon;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * Builder for configuring and managing the add-on system.
 * <p>
 * This builder provides configuration options for the overall add-on system state,
 * individual add-on settings, and add-ons that should be registered directly from
 * in-memory classes.
 * <p>
 * Individual add-on configurations are managed through {@link AddonBuilder} instances.
 * The builder also supports routing namespaced arguments to the corresponding
 * add-on builder.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see NamespacedBuilder
 * @see AddonBuilder
 * @see AddonSystemState
 * @see Addon
 * @since 4.0.0
 */
public class AddonSystemBuilder extends NamespacedBuilder<AddonSystemBuilder> {

    private final Map<String, AddonBuilder> addonBuilders;

    /**
     * Creates and initializes the add-on system builder.
     * <p>
     * The constructor assigns the {@code addons} namespace, registers this builder
     * with the root builder, initializes the collection of individual add-on builders,
     * and registers the supported arguments.
     * <p>
     * The initial state is {@link AddonSystemState#FULL}, and the in-memory add-on
     * collection is initialized as a synchronized list.
     *
     * @param root the root CraftsNet builder this add-on system builder belongs to
     */
    public AddonSystemBuilder(@NotNull CraftsNetBuilder root) {
        super(root, "addons");
        this.acquire(this);
        this.addonBuilders = new HashMap<>();

        this.addKnownArgument("state", ArgumentTransformer.enumType(AddonSystemState.class));
        this.addKnownArgument("in_memory", ArgumentTransformer.notAllowed("In memory addons can't be set via the cli"));

        this.state(AddonSystemState.FULL);
        this.set("in_memory", Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * Sets a configuration value either on this builder or on an individual add-on builder.
     * <p>
     * Arguments containing a colon are interpreted as add-on-specific configuration keys.
     * The part before the first colon identifies the add-on, while the remaining part
     * specifies the argument to set on that add-on's builder.
     *
     * @param arg   the configuration argument, optionally prefixed with an add-on namespace
     * @param value the value to assign to the argument
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code arg} or {@code value} is {@code null}
     */
    @Override
    protected @NotNull AddonSystemBuilder set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");

        if (!arg.contains(":")) {
            return super.set(arg, value);
        }

        String[] parts = arg.split(":", 2);
        addon(parts[0]).set(parts[1], value);
        return this;
    }

    /**
     * Retrieves the builder for the specified add-on and passes it to the supplied consumer.
     * <p>
     * If no builder exists for the add-on yet, one is created before the consumer is invoked.
     *
     * @param addon           the namespace identifying the add-on
     * @param builderConsumer the consumer that configures the add-on builder
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code addon} or {@code builderConsumer} is {@code null}
     */
    public AddonSystemBuilder addon(@NotNull String addon, Consumer<AddonBuilder> builderConsumer) {
        builderConsumer.accept(this.addon(addon));
        return this;
    }

    /**
     * Retrieves the builder associated with the specified add-on namespace.
     * <p>
     * Add-on namespaces are converted to lowercase before lookup. If no builder
     * exists, a new one is created unless this builder is frozen.
     *
     * @param addon the namespace identifying the add-on
     * @return the existing or newly created add-on builder, or {@code null} if this
     * builder is frozen and no builder exists for the namespace
     * @throws NullPointerException if {@code addon} is {@code null}
     */
    public AddonBuilder addon(@NotNull String addon) {
        Objects.requireNonNull(addon, "addon");

        return this.addonBuilders.computeIfAbsent(
                addon.toLowerCase(),
                namespace -> isFrozen() ? null : new AddonBuilder(this, namespace)
        );
    }

    /**
     * Sets the operating state of the add-on system.
     *
     * @param state the add-on system state to configure
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code state} is {@code null}
     * @see AddonSystemState
     */
    public @NotNull AddonSystemBuilder state(@NotNull AddonSystemState state) {
        return this.set("state", state);
    }

    /**
     * Retrieves the configured operating state of the add-on system.
     *
     * @return the configured add-on system state
     * @throws NullPointerException if no state is configured
     */
    public @NotNull AddonSystemState state() {
        return Objects.requireNonNull(
                getEnum("state", AddonSystemState.class),
                "No state for the addon system found"
        );
    }

    /**
     * Checks whether the add-on system is configured with the specified state.
     *
     * @param state the state to compare with the configured state
     * @return {@code true} if the configured state is identical to the specified state;
     * otherwise {@code false}
     * @throws NullPointerException if {@code state} is {@code null} or no state is configured
     */
    public boolean isState(@NotNull AddonSystemState state) {
        return this.state() == state;
    }

    /**
     * Retrieves the collection of add-on classes registered for in-memory loading.
     * <p>
     * The returned collection is the underlying collection stored in the builder,
     * so changes to it directly affect the registered in-memory add-ons.
     *
     * @return the collection of registered add-on classes
     * @throws NullPointerException if the in-memory add-on collection is not configured
     */
    @SuppressWarnings("rawtypes")
    public @NotNull Collection<Class> getInMemoryAddons() {
        return Objects
                .requireNonNull(
                        getCollection("in_memory", Class.class),
                        "No in memory addons present on the builder"
                );
    }

    /**
     * Registers one or more add-on classes for in-memory loading.
     *
     * @param addonTypes the add-on classes to register
     * @return this builder instance for method chaining
     * @throws NullPointerException if the array or collection passed to the delegated
     *                              overload is {@code null}
     * @see #addInMemoryAddon(Collection)
     */
    @SafeVarargs
    public final @NotNull AddonSystemBuilder addInMemoryAddon(@NotNull Class<? extends Addon>... addonTypes) {
        return this.addInMemoryAddon(Arrays.asList(addonTypes));
    }

    /**
     * Registers a collection of add-on classes for in-memory loading.
     * <p>
     * The supplied classes are appended to the collection returned by
     * {@link #getInMemoryAddons()}.
     *
     * @param addonTypes the add-on classes to register
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code addonTypes} is {@code null}
     */
    public @NotNull AddonSystemBuilder addInMemoryAddon(@NotNull Collection<Class<? extends Addon>> addonTypes) {
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
