package de.craftsblock.craftsnet.addon.meta;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.artifacts.ArtifactLoader;
import de.craftsblock.craftsnet.addon.loaders.AddonClassLoader;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.loaders.DependencyClassLoader;
import de.craftsblock.craftsnet.addon.meta.annotations.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents the configuration details of an {@link Addon}, including its metadata, classpath, and dependencies.
 * <p>
 * This class serves as a container for storing and managing the runtime configuration of addons,
 * ensuring proper dependency management and validation of addon metadata.
 * </p>
 *
 * @param path              The {@link Path} which contains the addon.
 * @param json              {@link Json} representation of the addon configuration.
 * @param classpath         Array of {@link URL}s representing the classpath of the addon.
 * @param dependencyLoaders An array of {@link DependencyClassLoader dependency class loaders} which
 *                          are used to load classes from the dependencies.
 * @param services          Collection of {@link RegisteredService} instances associated with the addon.
 * @param addon             Reference to the actual {@link Addon} instance.
 * @param meta              Reference to the {@link AddonMeta} metadata of the addon.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.1.0-SNAPSHOT
 */
public record AddonConfiguration(Path path, Json json, URL[] classpath, DependencyClassLoader[] dependencyLoaders,
                                 Collection<RegisteredService> services, AtomicReference<Addon> addon,
                                 AtomicReference<AddonMeta> meta, AtomicReference<AddonClassLoader> classLoader)
        implements Comparable<AddonConfiguration> {

    /**
     * Pattern for validating addon names.
     */
    public static final Pattern ADDON_NAME_VALIDATOR = Pattern.compile("^[a-zA-Z0-9\\-_.]{1,128}$");

    @ApiStatus.Internal
    private static final ConcurrentHashMap<Class<? extends Addon>, String> MAPPED_NAMES = new ConcurrentHashMap<>();

    /**
     * Creates an {@link AddonConfiguration} instance from the provided params.
     *
     * @param path              The {@link Path} which contains the addon.
     * @param json              {@link Json} representation of the addon configuration.
     * @param classpath         Array of {@link URL}s representing the classpath of the addon.
     * @param dependencyLoaders An array of {@link DependencyClassLoader dependency class loaders} which
     *                          are used to load classes from the dependencies.
     * @param services          Collection of {@link RegisteredService} instances associated with the addon.
     * @return A new instance of {@link AddonConfiguration}.
     */
    public static AddonConfiguration of(Path path, Json json, URL[] classpath, DependencyClassLoader[] dependencyLoaders,
                                        Collection<RegisteredService> services) {
        return new AddonConfiguration(path, json, classpath, dependencyLoaders, services,
                new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>());
    }

    /**
     * Creates a list of {@link AddonConfiguration} instances from the provided {@link Addon} class,
     * including its dependencies.
     *
     * @param craftsNet The instance of {@link CraftsNet} loading the addon for.
     * @param loader    The instance of {@link AddonLoader} which loads the addon.
     * @param addon     The addon class for which the configuration should be created.
     * @return A list of {@link AddonConfiguration}, including the configuration of the addon and its dependencies.
     * @throws IllegalStateException If the provided addon class is not annotated with {@link Meta}.
     */
    public static List<AddonConfiguration> of(CraftsNet craftsNet, AddonLoader loader, Class<? extends Addon> addon) {
        Meta meta = addon.getDeclaredAnnotation(Meta.class);
        if (meta == null && !MAPPED_NAMES.containsKey(addon)) {
            throw new IllegalStateException("The addon class " + addon.getName() + " is not annotated with @" + Meta.class.getSimpleName() + "!");
        }

        String name = MAPPED_NAMES.getOrDefault(addon, meta != null ? meta.name() : null);
        Depends depends = addon.getDeclaredAnnotation(Depends.class);

        List<RegisteredService> services = new ArrayList<>();
        List<URL> classpath = new ArrayList<>();
        classpath.add(addon.getProtectionDomain().getCodeSource().getLocation());

        Json conf = Json.empty().set("name", ensureValidAddonName(name))
                .set("main", addon.getName());

        Set<Depends> classes = new HashSet<>();
        if (depends != null) {
            classes.add(depends);
        } else {
            DependsCollection collection = addon.getDeclaredAnnotation(DependsCollection.class);
            if (collection != null) {
                classes.addAll(List.of(collection.value()));
            }
        }

        List<AddonConfiguration> configurations = new ArrayList<>();
        for (Depends depend : classes) {
            List<AddonConfiguration> subs = of(craftsNet, loader, depend.value());
            if (subs.isEmpty()) {
                continue;
            }
            configurations.addAll(subs);

            AddonConfiguration subConfig = subs.get(subs.size() - 1);
            conf.set((depend.soft() ? "softD" : "d") + "epends.$new", subConfig.json().get("name"));
            classpath.addAll(Arrays.stream(subConfig.classpath()).collect(Collectors.toSet()));
        }

        // Create a new artifact loader
        ArtifactLoader artifactLoader = new ArtifactLoader();

        Shadow shadow = addon.getDeclaredAnnotation(Shadow.class);
        EnumMap<ShadowType, List<String>> shadows = new EnumMap<>(ShadowType.class);
        if (shadow != null) {
            shadows.computeIfAbsent(shadow.type(), s -> new ArrayList<>()).add(shadow.value());
        } else {
            ShadowCollection collection = addon.getDeclaredAnnotation(ShadowCollection.class);
            if (collection != null) {
                for (Shadow nested : collection.value()) {
                    shadows.computeIfAbsent(nested.type(), s -> new ArrayList<>()).add(nested.value());
                }
            }
        }

        var mavenRepos = shadows.get(ShadowType.REPOSITORY);
        if (mavenRepos != null && !mavenRepos.isEmpty()) {
            artifactLoader.setup();
            for (String repo : mavenRepos) {
                artifactLoader.addRepository(repo);
            }
        }

        // Load all required dependencies
        URL[] dependencies;
        var mavenDependencies = shadows.get(ShadowType.DEPENDENCY);
        if (mavenDependencies != null && !mavenDependencies.isEmpty()) {
            artifactLoader.setup();
            dependencies = artifactLoader.loadLibraries(
                    craftsNet, loader,
                    services, name,
                    shadows.get(ShadowType.DEPENDENCY).toArray(String[]::new)
            );
        } else {
            dependencies = new URL[0];
        }

        artifactLoader.stop();

        DependencyClassLoader[] dependencyClassLoaders = Arrays.stream(dependencies)
                .map(url -> DependencyClassLoader.safelyNew(craftsNet, url))
                .toArray(DependencyClassLoader[]::new);
        classpath.addAll(Arrays.stream(dependencies).collect(Collectors.toSet()));

        configurations.add(of(null, conf, classpath.toArray(URL[]::new), dependencyClassLoaders, services));
        return configurations;
    }

    /**
     * Compares this configuration to another based on their addon names.
     *
     * @param o The other {@link AddonConfiguration}.
     * @return A negative integer, zero, or a positive integer as this configuration's name
     * is lexicographically less than, equal to, or greater than the other configuration's name.
     */
    @Override
    public int compareTo(@NotNull AddonConfiguration o) {
        return this.json().getString("name").compareTo(o.json().getString("name"));
    }

    /**
     * Sanitizes the name of the addon through checking the name against the {@link AddonConfiguration#ADDON_NAME_VALIDATOR}.
     *
     * @param name The name of the addon.
     * @return The sanitized name of the addon.
     * @throws IllegalArgumentException If the addon name does not match with {@link AddonConfiguration#ADDON_NAME_VALIDATOR}.
     */
    public static String ensureValidAddonName(String name) {
        if (ADDON_NAME_VALIDATOR.matcher(name).matches()) {
            return name;
        }

        throw new IllegalArgumentException("Invalid addon name: " + name.substring(0, 128) + ". " +
                "Only letters, numbers, hyphens, underscores and dots are allowed, " +
                "up to 128 characters.");
    }

    /**
     * Maps an addon class to a custom name.
     *
     * @param addon The addon class.
     * @param name  The name to associate with the addon class.
     */
    @ApiStatus.Internal
    public static void map(Class<? extends Addon> addon, String name) {
        MAPPED_NAMES.put(addon, name);
    }

}