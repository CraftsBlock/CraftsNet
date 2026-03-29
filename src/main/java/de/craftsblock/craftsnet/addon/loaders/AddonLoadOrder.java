package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftsnet.addon.Addon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Closeable;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a system for managing the load order of addons.
 *
 * <p>This implementation is based on a directed dependency graph and uses
 * a topological sorting algorithm (Kahn's Algorithm) to determine a valid
 * and efficient load order.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.2-SNAPSHOT
 */
public final class AddonLoadOrder implements Closeable {

    /**
     * Stores all known addons mapped by their name.
     */
    private final Map<String, BootMapping> addonLoadOrder =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Stores required dependencies.
     */
    private final Map<String, Set<String>> dependencies = new HashMap<>();

    /**
     * Stores optional dependencies.
     */
    private final Map<String, Set<String>> softDependencies = new HashMap<>();

    /**
     * Adds an Addon to the system.
     *
     * @param addon The Addon to add
     */
    public void addAddon(Addon addon) {
        addonLoadOrder.compute(
                addon.getName(),
                (name, mapping) -> {
                    if (mapping == null) {
                        return new BootMapping(name, addon, true);
                    }

                    return mapping.setAddon(addon);
                }
        );
    }

    /**
     * Clears all stored data.
     */
    @Override
    public void close() {
        addonLoadOrder.clear();
        dependencies.clear();
        softDependencies.clear();
    }

    /**
     * Checks if the given addon exists.
     *
     * @param addon The addon instance
     * @return true if present
     */
    public boolean contains(Addon addon) {
        return contains(addon.getName());
    }

    /**
     * Checks if the given addon exists.
     *
     * @param addon The addon name
     * @return true if present
     */
    public boolean contains(String addon) {
        return addonLoadOrder.containsKey(addon)
                && addonLoadOrder.get(addon).getAddon() != null;
    }

    /**
     * Registers a required dependency.
     *
     * @param addon     The addon declaring the dependency
     * @param dependsOn The required dependency
     */
    public void depends(String addon, String dependsOn) {
        dryDepends(addon, dependsOn, dependencies, true);
    }

    /**
     * Registers an optional dependency.
     *
     * @param addon     The addon declaring the dependency
     * @param dependsOn The optional dependency
     */
    public void softDepends(String addon, String dependsOn) {
        this.dryDepends(addon, dependsOn, softDependencies, false);
    }

    /**
     * Registers a dependency with the specified required state.
     *
     * @param addon        The addon declaring the dependency.
     * @param dependsOn    The dependency.
     * @param dependencies The dependencies the dependency should be registered to.
     * @param required     Whether the dependency is forcibly required.
     */
    private void dryDepends(String addon, String dependsOn,
                            Map<String, Set<String>> dependencies,
                            boolean required) {
        validateSelfDependency(addon, dependsOn);

        dependencies.computeIfAbsent(addon, k -> new HashSet<>())
                .add(dependsOn);

        addonLoadOrder.putIfAbsent(addon, new BootMapping(addon, null, false));

        if (addonLoadOrder.containsKey(dependsOn)) {
            addonLoadOrder.get(dependsOn).mergeRequired(required);
        } else {
            addonLoadOrder.put(dependsOn, new BootMapping(dependsOn, null, required));
        }
    }

    /**
     * Creates a new sorted stream of the currently registered
     * {@link BootMapping}'s.
     *
     * @return The created and sorted stream.
     */
    private @Unmodifiable Stream<BootMapping> createSortedBootMappingStream() {
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (String name : addonLoadOrder.keySet()) {
            graph.putIfAbsent(name, new ArrayList<>());
            inDegree.putIfAbsent(name, 0);
        }

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String addon = entry.getKey();

            for (String dep : entry.getValue()) {
                if (!addonLoadOrder.containsKey(dep)) {
                    throw new IllegalStateException("Missing required dependency: " + dep);
                }

                graph.computeIfAbsent(dep, k -> new ArrayList<>()).add(addon);
                inDegree.put(addon, inDegree.getOrDefault(addon, 0) + 1);
            }
        }

        for (Map.Entry<String, Set<String>> entry : softDependencies.entrySet()) {
            String addon = entry.getKey();

            for (String dep : entry.getValue()) {
                if (!addonLoadOrder.containsKey(dep)) {
                    continue;
                }

                graph.computeIfAbsent(dep, k -> new ArrayList<>()).add(addon);
                inDegree.put(addon, inDegree.getOrDefault(addon, 0) + 1);
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(current);

            for (String next : graph.getOrDefault(current, Collections.emptyList())) {
                int newDegree = inDegree.merge(next, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(next);
                }
            }
        }

        if (sorted.size() != addonLoadOrder.size()) {
            throw new IllegalStateException("Dependency cycle detected!");
        }

        return sorted.stream().map(addonLoadOrder::get);
    }

    /**
     * Builds and returns the final load order.
     *
     * <p>This method performs a topological sort over the dependency graph.</p>
     *
     * @return ordered list of addons
     * @throws IllegalStateException if dependencies are missing or cyclic
     */
    public @Unmodifiable List<String> getPreLoadOrder() {
        return createSortedBootMappingStream()
                .filter(Objects::nonNull)
                .map(BootMapping::getName)
                .toList();
    }

    /**
     * Builds and returns the final load order.
     *
     * <p>This method performs a topological sort over the dependency graph.</p>
     *
     * @return ordered list of addons
     * @throws IllegalStateException if dependencies are missing or cyclic
     */
    public @Unmodifiable List<Addon> getLoadOrder() {
        return createSortedBootMappingStream()
                .filter(Objects::nonNull)
                .filter(BootMapping::presenceFilter)
                .map(BootMapping::getAddon)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Validates that an addon does not depend on itself.
     */
    private void validateSelfDependency(String addon, String dependsOn) {
        if (addon.equalsIgnoreCase(dependsOn)) {
            throw new IllegalStateException("Can not add " + addon + " as depends to itself!");
        }
    }

    /**
     * Represents an internal mapping for an addon.
     */
    private static class BootMapping {

        private final String name;
        private boolean required;
        private Addon addon;

        /**
         * Constructs a BootMapping for the specified addon.
         *
         * @param name     The name of the addon.
         * @param addon    The addon associated with this mapping.
         * @param required Whether this {@link BootMapping} is required to properly start or not.
         */
        public BootMapping(@NotNull String name,
                           @Nullable Addon addon,
                           boolean required) {
            this.name = name;
            this.addon = addon;
            this.required = required;
        }

        /**
         * Gets the addon name associated with this mapping.
         *
         * @return The addon name.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the addon associated with this mapping.
         *
         * @param addon The addon to be associated with this mapping.
         * @return The modified BootMapping instance with the updated addon.
         */
        public BootMapping setAddon(Addon addon) {
            this.addon = addon;
            return this;
        }

        /**
         * Retrieves the addon associated with this mapping.
         *
         * @return The addon associated with this mapping.
         */
        public Addon getAddon() {
            return addon;
        }

        /**
         * Merges the required state into this mapping.
         *
         * @param required The required state to apply if this mapping is not already required.
         */
        public void mergeRequired(boolean required) {
            if (this.required) {
                return;
            }

            this.required = required;
        }

        /**
         * Ensures required addons are present.
         *
         * @return true if valid
         */
        public boolean presenceFilter() {
            if (!required || addon != null) {
                return true;
            }

            throw new IllegalStateException(
                    "The addon " + this.name + " is required but not found!"
            );
        }
    }
}