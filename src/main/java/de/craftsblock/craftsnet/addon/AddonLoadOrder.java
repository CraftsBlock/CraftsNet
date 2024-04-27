package de.craftsblock.craftsnet.addon;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a system for managing the load order of addons.
 * This class may be used to organize and control the order in which addons are loaded or executed.
 * It provides methods for adding addons, specifying dependencies, and retrieving the load order.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-3.0.2
 */
final class AddonLoadOrder {

    private final ConcurrentHashMap<String, BootMapping> addonLoadOrder = new ConcurrentHashMap<>();

    /**
     * Adds an Addon to the system, considering its load order.
     * The method utilizes a ConcurrentHashMap to efficiently manage the load order of addons based on their names.
     * If an addon with the same name already exists, the load order is updated accordingly.
     *
     * @param addon The Addon object to be added to the system.
     */
    public void addAddon(Addon addon) {
        addonLoadOrder.compute(addon.getName(), (name, bootMapping) ->
                (bootMapping == null) ? new BootMapping(0, addon) : bootMapping.addon(addon));
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
     * Specifies a dependency relationship between two addons, influencing their load order.
     * The method uses a ConcurrentHashMap to manage the load order of addons based on their dependencies.
     * If the specified dependency does not exist, a new entry is created with a default BootMapping.
     * The load order is adjusted according to the priorities of the dependent addons.
     *
     * @param addon     The Addon object representing the dependent addon.
     * @param dependsOn The name of the addon on which the specified addon depends.
     */
    public void depends(Addon addon, String dependsOn) {
        addonLoadOrder.merge(dependsOn, new BootMapping(0, null),
                (existingMapping, newMapping) -> {
                    int addonPriority = getPriority(addon.getName());
                    int dependsOnPriority = getPriority(dependsOn);
                    return (addonPriority <= dependsOnPriority) ?
                            existingMapping.priority(dependsOnPriority + 1) :
                            existingMapping;
                });
    }

    /**
     * Retrieves an unmodifiable collection representing the load order of addons.
     * The method generates a sorted list of addon names based on their load priorities,
     * and then maps the names to their corresponding addons using the addonLoadOrder map.
     * The resulting list of addons is filtered to exclude any null values, and the final collection is returned.
     *
     * @return An unmodifiable Collection of addons, representing the load order.
     */
    public @Unmodifiable Collection<Addon> getLoadOrder() {
        List<String> bootOrderList = new ArrayList<>(addonLoadOrder.keySet());
        bootOrderList.sort(Comparator.comparingInt(value -> addonLoadOrder.get(value.toString()).priority()).reversed());

        return bootOrderList.parallelStream()
                .map(addonLoadOrder::get)
                .filter(Objects::nonNull)
                .map(BootMapping::addon)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Retrieves the priority of an addon in the load order.
     * If the addon is not found in the load order, a default BootMapping with priority 0 is used.
     *
     * @param addon The name of the addon for which the priority is to be retrieved.
     * @return The priority of the specified addon in the load order.
     */
    private int getPriority(String addon) {
        return addonLoadOrder.getOrDefault(addon, new BootMapping(0, null)).priority();
    }

    /**
     * Represents a mapping used in the context of addon load order, associating a priority level with an addon.
     * This class is utilized for organizing and managing the load order of addons within the system.
     */
    private static class BootMapping {

        private int priority;
        private Addon addon;

        /**
         * Constructs a BootMapping with the specified priority and addon.
         *
         * @param priority The priority level assigned to the addon in the load order.
         * @param addon    The addon associated with this mapping.
         */
        public BootMapping(int priority, @Nullable Addon addon) {
            this.priority = priority;
            this.addon = addon;
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
    }
}
