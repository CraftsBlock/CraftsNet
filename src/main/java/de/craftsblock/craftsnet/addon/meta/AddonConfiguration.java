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
 * @version 1.4.0
 * @since 3.1.0-SNAPSHOT
 */
public record AddonConfiguration(Path path, Json json, URL[] classpath, DependencyClassLoader[] dependencyLoaders,
                                 Collection<RegisteredService> services, AtomicReference<Addon> addon,
                                 AtomicReference<AddonMeta> meta, AtomicReference<AddonClassLoader> classLoader)
        implements Comparable<AddonConfiguration> {

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
        if (meta == null && !MAPPED_NAMES.containsKey(addon))
            throw new IllegalStateException("The addon class " + addon.getName() + " is not annotated with @" + Meta.class.getSimpleName() + "!");

        String name = MAPPED_NAMES.getOrDefault(addon, meta != null ? meta.name() : null);
        Depends depends = addon.getDeclaredAnnotation(Depends.class);

        List<RegisteredService> services = new ArrayList<>();
        List<URL> classpath = new ArrayList<>();
        classpath.add(addon.getProtectionDomain().getCodeSource().getLocation());

        Json conf = Json.empty().set("name", sanitizeName(name))
                .set("main", addon.getName());

        Set<Depends> classes = new HashSet<>();
        if (depends != null) classes.add(depends);
        else {
            DependsCollection collection = addon.getDeclaredAnnotation(DependsCollection.class);
            if (collection != null) classes.addAll(List.of(collection.value()));
        }

        List<AddonConfiguration> configurations = new ArrayList<>();
        for (Depends depend : classes) {
            List<AddonConfiguration> subs = of(craftsNet, loader, depend.value());
            if (subs.isEmpty()) continue;
            configurations.addAll(subs);

            AddonConfiguration subConfig = subs.get(subs.size() - 1);
            conf.set((depend.soft() ? "softD" : "d") + "epends.$new", subConfig.json().get("name"));
            classpath.addAll(Arrays.stream(subConfig.classpath()).collect(Collectors.toSet()));
        }

        // Create a new artifact loader
        ArtifactLoader artifactLoader = new ArtifactLoader();

        Shadow shadow = addon.getDeclaredAnnotation(Shadow.class);
        EnumMap<ShadowType, List<String>> shadows = new EnumMap<>(ShadowType.class);
        if (shadow != null) shadows.computeIfAbsent(shadow.type(), s -> new ArrayList<>()).add(shadow.value());
        else {
            ShadowCollection collection = addon.getDeclaredAnnotation(ShadowCollection.class);
            if (collection != null)
                for (Shadow nested : collection.value())
                    shadows.computeIfAbsent(nested.type(), s -> new ArrayList<>()).add(nested.value());
        }

        // Inject all repositories
        if (shadows.containsKey(ShadowType.REPOSITORY))
            for (String repo : shadows.get(ShadowType.REPOSITORY))
                artifactLoader.addRepository(repo);

        // Load all required dependencies
        URL[] dependencies;
        if (shadows.containsKey(ShadowType.DEPENDENCY))
            dependencies = artifactLoader.loadLibraries(craftsNet, loader, services, name, shadows.get(ShadowType.DEPENDENCY).toArray(String[]::new));
        else dependencies = new URL[0];

        classpath.addAll(Arrays.stream(dependencies).collect(Collectors.toSet()));
        artifactLoader.stop();

        configurations.add(of(null, conf, classpath.toArray(URL[]::new), new DependencyClassLoader[0], services));
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
     * Pattern for validating addon names.
     * <p>Addon names can only contain alphanumeric characters.</p>
     */
    public static final Pattern NAME_VALIDATOR = Pattern.compile("^[a-zA-Z0-9]*$");

    /**
     * Sanitizes the name of the addon through checking the name against the {@link AddonConfiguration#NAME_VALIDATOR}.
     *
     * @param name The name of the addon.
     * @return The sanitized name of the addon.
     * @throws IllegalArgumentException If the addon name does not match with {@link AddonConfiguration#NAME_VALIDATOR}.
     */
    private static String sanitizeName(String name) {
        if (NAME_VALIDATOR.matcher(name).matches()) return name;
        throw new IllegalArgumentException("Addon names must not contain special characters / spaces! Provided: \"" + name + "\"");
    }

    /**
     * Maps an addon class to a custom name.
     *
     * @param addon The addon class.
     * @param name  The name to associate with the addon class.
     */
    public static void map(Class<? extends Addon> addon, String name) {
        MAPPED_NAMES.put(addon, name);
    }

}