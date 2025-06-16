package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.autoregister.loaders.AutoRegisterLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a system for managing the load order of addons.
 * This class may be used to organize and control the order in which addons are loaded or executed.
 * It provides methods for adding addons, specifying dependencies, and retrieving the load order.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
 * @since 3.0.2-SNAPSHOT
 */
final class AddonLoadOrder implements Closeable {

    private final Map<String, BootMapping> addonLoadOrder = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Adds an Addon to the system, considering its load order.
     * The method utilizes a ConcurrentHashMap to efficiently manage the load order of addons based on their names.
     * If an addon with the same name already exists, the load order is updated accordingly.
     *
     * @param addon The Addon object to be added to the system.
     */
    public void addAddon(Addon addon) {
        addonLoadOrder.compute(addon.getName(), (name, bootMapping) ->
                (bootMapping == null) ? new BootMapping(name, 0, addon, true) : bootMapping.addon(addon));
    }

    /**
     * Perform cleanup for the current {@link AddonLoadOrder}
     * when it is no longer used.
     */
    @Override
    public void close() {
        addonLoadOrder.clear();
    }

    /**
     * Checks if the specified addon is present in the addon load order and has a non-null association.
     *
     * @param addon The addon to be checked for presence.
     * @return true if the addon is present in the addon load order and has a non-null association, false otherwise.
     */
    public boolean contains(Addon addon) {
        return contains(addon.getName());
    }

    /**
     * Checks if the specified addon is present in the addon load order and has a non-null association.
     *
     * @param addon The addon to be checked for presence.
     * @return true if the addon is present in the addon load order and has a non-null association, false otherwise.
     */
    public boolean contains(String addon) {
        return addonLoadOrder.containsKey(addon) && addonLoadOrder.get(addon).addon() != null;
    }

    /**
     * Registers a required dependency for the specified addon.
     *
     * @param addon     the addon that declares the dependency
     * @param dependsOn the name of the addon that is depended on
     */
    public void depends(String addon, String dependsOn) {
        this.dryDepends(addon, dependsOn, true);
    }

    /**
     * Registers an optional (soft) dependency for the specified addon.
     *
     * @param addon     the addon that declares the dependency
     * @param dependsOn the name of the addon that is optionally depended on
     * @since 3.3.4-SNAPSHOT
     */
    public void softDepends(String addon, String dependsOn) {
        this.dryDepends(addon, dependsOn, false);
    }

    /**
     * Registers a dependency for the specified addon on another addon.
     * <p>
     * The dependency can be either required or optional based on the {@code required} parameter.
     * This method updates the internal addon load order by merging a new {@link BootMapping} into the existing mapping.
     * If a dependency mapping for {@code dependsOn} already exists, the method adjusts its priority and, if necessary,
     * marks it as required.
     * </p>
     *
     * @param addon     the addon that declares the dependency
     * @param dependsOn the name of the addon that is being depended on
     * @param required  {@code true} if the dependency is required, {@code false} otherwise.
     * @throws IllegalStateException if the addon attempts to depend on itself
     * @since 3.3.4-SNAPSHOT
     */
    private void dryDepends(String addon, String dependsOn, boolean required) {
        if (addon.equalsIgnoreCase(dependsOn))
            throw new IllegalStateException("Can not add " + addon + " as depends to itself!");

        final int addonPriority = getPriority(addon);
        addonLoadOrder.merge(dependsOn, new BootMapping(dependsOn, addonPriority + 1, null, required),
                (existingMapping, newMapping) -> {
                    int dependsOnPriority = getPriority(dependsOn);
                    if (required) existingMapping.require();
                    return (addonPriority <= dependsOnPriority) ?
                            existingMapping.priority(addonPriority + 1) :
                            existingMapping;
                });
    }

    /**
     * Retrieves an unmodifiable collection representing the load order of addons names.
     * The method generates a sorted list of addon names based on their load priorities,
     * and then maps the names to their corresponding addons using the addonLoadOrder map.
     *
     * @return An unmodifiable Collection of addons, representing the load order.
     * @since 3.4.3
     */
    public @Unmodifiable List<String> getPreLoadOrder() {
        List<String> bootOrderList = new ArrayList<>(addonLoadOrder.keySet());
        bootOrderList.sort(Comparator.comparingInt(value -> addonLoadOrder.get(value.toString()).priority()).reversed());
        return bootOrderList;
    }

    /**
     * Retrieves an unmodifiable collection representing the load order of addons.
     * The resulting list of addons is filtered to exclude any null values, and the final collection is returned.
     *
     * @return An unmodifiable Collection of addons, representing the load order.
     */
    public @Unmodifiable List<Addon> getLoadOrder() {
        return getPreLoadOrder().stream()
                .map(addonLoadOrder::get)
                .filter(Objects::nonNull)
                .filter(BootMapping::presenceFilter)
                .sorted((o1, o2) -> Integer.compare(o2.priority(), o1.priority()))
                .map(BootMapping::addon)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Retrieves the priority of an addon in the load order.
     * If the addon is not found in the load order, a default BootMapping with priority 0 is used.
     *
     * @param addon The name of the addon for which the priority is to be retrieved.
     * @return The priority of the specified addon in the load order.
     */
    private int getPriority(String addon) {
        return addonLoadOrder.getOrDefault(addon, new BootMapping(addon, 0, null, false)).priority();
    }

    /**
     * Represents a mapping used in the context of addon load order, associating a priority level with an addon.
     * This class is utilized for organizing and managing the load order of addons within the system.
     */
    private static class BootMapping {

        private final String name;
        private int priority;
        private Addon addon;
        private boolean required;

        /**
         * Constructs a BootMapping with the specified priority and addon.
         *
         * @param name     The name of the addon.
         * @param priority The priority level assigned to the addon in the load order.
         * @param addon    The addon associated with this mapping.
         * @param required Whether this {@link BootMapping} is required to properly start or not.
         */
        public BootMapping(@NotNull String name, int priority, @Nullable Addon addon, boolean required) {
            this.name = name;
            this.priority = priority;
            this.addon = addon;
            this.required = required;
        }

        /**
         * Sets the priority level for the addon in the load order.
         *
         * @param priority The new priority level to be set.
         * @return The modified BootMapping instance with the updated priority.
         */
        public BootMapping priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the addon associated with this mapping.
         *
         * @param addon The addon to be associated with this mapping.
         * @return The modified BootMapping instance with the updated addon.
         */
        public BootMapping addon(Addon addon) {
            this.addon = addon;
            return this;
        }

        /**
         * Marks this {@link BootMapping} as required to properly start.
         *
         * @since 3.3.4-SNAPSHOT
         */
        public void require() {
            this.required = true;
        }

        /**
         * Retrieves the priority level assigned to the addon in the load order.
         *
         * @return The priority level of the addon in the load order.
         */
        public int priority() {
            return priority;
        }

        /**
         * Retrieves the addon associated with this mapping.
         *
         * @return The addon associated with this mapping.
         */
        public Addon addon() {
            return addon;
        }

        /**
         * Filter for the presence check of this {@link BootMapping}.
         *
         * @return {@code true} if the {@link BootMapping} is properly present.
         * @throws IllegalStateException If the addon is required but not present.
         * @since 3.3.4-SNAPSHOT
         */
        public boolean presenceFilter() {
            if (!required) return true;
            if (addon != null) return true;

            throw new IllegalStateException("The addon \"" + this.name + "\" is required but not found!");
        }

    }
}
