package de.craftsblock.craftsnet.addon.meta;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.meta.annotations.Depends;
import de.craftsblock.craftsnet.addon.meta.annotations.DependsCollection;
import de.craftsblock.craftsnet.addon.meta.annotations.Meta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
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
 * @param json      {@link Json} representation of the addon configuration.
 * @param classpath Array of {@link URL}s representing the classpath of the addon.
 * @param services  Collection of {@link RegisteredService} instances associated with the addon.
 * @param addon     Reference to the actual {@link Addon} instance.
 * @param meta      Reference to the {@link AddonMeta} metadata of the addon.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public record AddonConfiguration(Json json, URL[] classpath, Collection<RegisteredService> services, AtomicReference<Addon> addon,
                                 AtomicReference<AddonMeta> meta) implements Comparable<AddonConfiguration> {

    @ApiStatus.Internal
    private static final ConcurrentHashMap<Class<? extends Addon>, String> MAPPED_NAMES = new ConcurrentHashMap<>();

    /**
     * Creates an {@link AddonConfiguration} instance from the provided params.
     *
     * @param json      Content of the addon.json
     * @param classpath Classpath of the jar file
     * @param services  Services that should be registered
     * @return A new instance of {@link AddonConfiguration}.
     */
    public static AddonConfiguration of(Json json, URL[] classpath, Collection<RegisteredService> services) {
        return new AddonConfiguration(json, classpath, services, new AtomicReference<>(), new AtomicReference<>());
    }

    /**
     * Creates a list of {@link AddonConfiguration} instances from the provided {@link Addon} class,
     * including its dependencies.
     *
     * @param addon The addon class for which the configuration should be created.
     * @return A list of {@link AddonConfiguration}, including the configuration of the addon and its dependencies.
     * @throws IllegalStateException If the provided addon class is not annotated with {@link Meta}.
     */
    public static List<AddonConfiguration> of(Class<? extends Addon> addon) {
        Meta meta = addon.getDeclaredAnnotation(Meta.class);
        if (meta == null && !MAPPED_NAMES.containsKey(addon))
            throw new IllegalStateException("The addon class " + addon.getName() + " is not annotated with @" + Meta.class.getSimpleName() + "!");

        String name = MAPPED_NAMES.getOrDefault(addon, meta != null ? meta.name() : null);
        Depends depends = addon.getDeclaredAnnotation(Depends.class);

        List<URL> classpath = new ArrayList<>();
        classpath.add(addon.getProtectionDomain().getCodeSource().getLocation());

        Json conf = Json.empty().set("name", sanitizeName(name))
                .set("main", addon.getName());

        Set<Class<? extends Addon>> classes = new HashSet<>();
        if (depends != null) classes.add(depends.value());
        else {
            DependsCollection collection = addon.getDeclaredAnnotation(DependsCollection.class);
            if (collection != null) classes.addAll(Arrays.stream(collection.value()).map(Depends::value).toList());
        }

        List<AddonConfiguration> configurations = new ArrayList<>();
        for (Class<? extends Addon> depend : classes) {
            List<AddonConfiguration> subs = of(depend);
            if (subs.isEmpty()) continue;
            configurations.addAll(subs);

            AddonConfiguration subConfig = subs.get(subs.size() - 1);
            conf.set("depends.$new", subConfig.json().get("name"));
            classpath.addAll(Arrays.stream(subConfig.classpath()).collect(Collectors.toSet()));
        }

        configurations.add(of(conf, classpath.toArray(URL[]::new), Collections.emptyList()));
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