package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.addon.HollowAddon;
import de.craftsblock.craftsnet.addon.artifacts.ArtifactLoader;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.addon.meta.AddonMeta;
import de.craftsblock.craftsnet.addon.meta.RegisteredService;
import de.craftsblock.craftsnet.addon.meta.Startup;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.autoregister.loaders.AutoRegisterLoader;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;
import de.craftsblock.craftsnet.events.addons.AllAddonsLoadedEvent;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * The AddonLoader class is responsible for loading and managing addons in the application.
 * It loads addon jar files, extracts necessary information, and initializes addon instances.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.3.2
 * @see Addon
 * @see AddonManager
 * @since 1.0.0-SNAPSHOT
 */
public final class AddonLoader {

    private final Stack<Path> addons = new Stack<>();
    private final CraftsNet craftsNet;
    private final Logger logger;

    /**
     * Constructs a new instance of an addon loader
     *
     * @param craftsNet The CraftsNet instance which instantiates this
     */
    public AddonLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.getLogger();
    }

    /**
     * Adds a new addon file to the loader using the file name.
     *
     * @param file The name of the addon file.
     */
    public void update(String file) {
        update(Path.of("addons", file));
    }

    /**
     * Adds a new addon to the loader using the path to the file.
     *
     * @param path The addon path to add.
     */
    public void update(Path path) {
        try {
            if (path.getParent() != null && Files.notExists(path.getParent()))
                Files.createDirectories(path.getParent());

            if (Files.notExists(path))
                throw new NullPointerException("The path (%s) does not exist!".formatted(
                        path.toFile().getAbsolutePath()
                ));

            synchronized (addons) {
                if (!addons.contains(path))
                    addons.push(path);
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not load addon from path %s".formatted(
                    path.toFile().getAbsolutePath()
            ), e);
        }
    }

    /**
     * Clears all addon paths from the stack.
     *
     * @since 3.4.3
     */
    public void reset() {
        synchronized (addons) {
            addons.clear();
        }
    }

    /**
     * Loads all the addons from the provided addon files.
     *
     * @throws IOException if there is an I/O error while loading the addons.
     */
    public List<AddonConfiguration> load() throws IOException {
        Logger logger = craftsNet.getLogger();

        // Create a new artifact loader
        ArtifactLoader artifactLoader = new ArtifactLoader();

        // Load all the dependencies and repositories from the addons
        List<AddonConfiguration> configurations = new ArrayList<>();
        synchronized (addons) {
            for (Path path : addons) {
                if (Files.isDirectory(path)) continue;

                try (JarFile jarFile = new JarFile(path.toFile(), true, ZipFile.OPEN_READ, Runtime.version())) {
                    logger.debug("Loading jar file " + path.toFile().getAbsolutePath());

                    // Load the configuration file from the jar
                    AddonConfiguration configuration = retrieveConfig(path, jarFile);
                    if (configuration == null) {
                        logger.error(new FileNotFoundException("Could not locate the addon.json within " + path.toFile().getPath() + "!"));
                        continue;
                    }
                    Json addon = configuration.json();
                    String name = addon.getString("name");

                    // Check if the jar version is compatible
                    long checkStart = System.currentTimeMillis();

                    try {
                        compatibleOrThrow(jarFile);
                        logger.debug(path.toFile().getAbsolutePath() + " is jvm compatible, checked within " + (System.currentTimeMillis() - checkStart) + "ms");
                    } catch (RuntimeException e) {
                        logger.error(e);
                        logger.error(path.toFile().getAbsolutePath() + " is not jvm compatible, checked within " + (System.currentTimeMillis() - checkStart) + "ms");
                        continue;
                    }

                    // Inject all repositories
                    artifactLoader.cleanup();
                    if (addon.contains("repositories"))
                        for (String repo : addon.getStringList("repositories"))
                            artifactLoader.addRepository(repo);

                    // Load all required dependencies
                    URL[] dependencies;
                    if (addon.contains("dependencies"))
                        dependencies = artifactLoader.loadLibraries(this.craftsNet, this, configuration.services(),
                                name, addon.getStringList("dependencies").toArray(String[]::new));
                    else dependencies = new URL[0];

                    DependencyClassLoader[] dependencyClassLoaders = Arrays.stream(dependencies)
                            .map(url -> DependencyClassLoader.safelyNew(craftsNet, url)).toArray(DependencyClassLoader[]::new);

                    // Generate classpath
                    URL[] classpath = new URL[]{path.toUri().toURL()};

                    // Put the configuration in the configurations map
                    configurations.add(new AddonConfiguration(path, configuration.json(), classpath, dependencyClassLoaders,
                            configuration.services(), configuration.addon(), configuration.meta(), configuration.classLoader()));
                }
            }

            addons.clear();
        }

        artifactLoader.stop();
        return configurations;
    }

    /**
     * Loads all the addons from the provided list of {@link AddonConfiguration}.
     *
     * @param rawConfigurations The list of {@link AddonConfiguration}.
     */
    public void load(List<AddonConfiguration> rawConfigurations) {
        // Ensure that rawConfigurations is an array list
        List<AddonConfiguration> configurations;
        if (rawConfigurations instanceof ArrayList<AddonConfiguration>)
            configurations = rawConfigurations;
        else configurations = new ArrayList<>(rawConfigurations);

        AddonLoadOrder loadOrder = new AddonLoadOrder();
        HashMap<URI, Map.Entry<JarFile, ArrayList<Addon>>> codesSources = new HashMap<>();

        // Pre setup tasks
        configurations.forEach(configuration -> {
            configuration.meta().set(AddonMeta.of(configuration));

            configuration.classLoader().set(new AddonClassLoader(this.craftsNet, configuration));
            preBuildLoadOrder(loadOrder, configuration);
        });

        sortConfigurations(loadOrder, configurations);

        // Initialize tasks
        configurations.forEach(configuration -> {
            Addon addon = instantiateAddon(configuration);
            if (addon == null) return;

            craftsNet.getAddonManager().register(addon);
            configuration.addon().set(addon);

            addToLoadOrder(loadOrder, addon);
            addCodeSource(configuration, addon, codesSources);
        });

        configurations.forEach(this::loadServices);

        HashMap<Addon, List<AutoRegisterInfo>> autoRegisterInfos;
        try (AutoRegisterLoader autoRegisterLoader = new AutoRegisterLoader()) {
            autoRegisterInfos = convertToAutoRegister(autoRegisterLoader, codesSources);
        }

        Collection<Addon> orderedLoad = enableAddons(loadOrder, autoRegisterInfos);

        autoRegisterInfos.clear();
        orderedLoad.clear();
        loadOrder.close();

        try {
            craftsNet.getListenerRegistry().call(new AllAddonsLoadedEvent());
        } catch (Exception e) {
            logger.error(e, "Can not fire addons loaded event!");
        }
    }

    /**
     * Loads and enables all addons that have been found.
     *
     * @param loadOrder         The {@link AddonLoadOrder load order} in which the addons will be loaded.
     * @param autoRegisterInfos A list of {@link AutoRegisterInfo} which should be applied for the addons.
     * @return The list of all loaded addons.
     * @since 3.4.4
     */
    private @NotNull Collection<Addon> enableAddons(AddonLoadOrder loadOrder, HashMap<Addon, List<AutoRegisterInfo>> autoRegisterInfos) {
        Collection<Addon> orderedLoad = loadOrder.getLoadOrder();

        // Loading all addons
        orderedLoad.forEach(addon -> {
            logger.info("Loading addon " + addon.getName() + "...");
            addon.onLoad();

            if (!autoRegisterInfos.containsKey(addon)) return;
            craftsNet.getAutoRegisterRegistry().handleAll(autoRegisterInfos.get(addon), Startup.LOAD);
        });

        // Enabling all addons
        orderedLoad.forEach(addon -> {
            logger.info("Enabling addon " + addon.getName() + "...");
            addon.onEnable();

            if (!autoRegisterInfos.containsKey(addon)) return;
            craftsNet.getAutoRegisterRegistry().handleAll(autoRegisterInfos.get(addon), Startup.ENABLE);
        });

        return orderedLoad;
    }

    /**
     * Sorts the configurations according to the load order.
     *
     * @param loadOrder      The load order which holds the order of the addons.
     * @param configurations The configurations to sort
     * @since 3.4.3
     */
    private void sortConfigurations(AddonLoadOrder loadOrder, List<AddonConfiguration> configurations) {
        var orderedConfigurationNames = loadOrder.getPreLoadOrder();
        Map<String, Integer> sortMap = new HashMap<>();
        for (int i = 0; i < orderedConfigurationNames.size(); i++)
            sortMap.put(orderedConfigurationNames.get(i), i);

        configurations.sort(Comparator.comparingInt(c -> sortMap.getOrDefault(c.meta().get().name(), Integer.MAX_VALUE)));
    }

    /**
     * Pre-building the load order with just the names of the addons.
     *
     * @param loadOrder     The load order which stores the order of the addons.
     * @param configuration The addon configuration which should be added to the load order.
     * @since 3.4.3
     */
    private void preBuildLoadOrder(AddonLoadOrder loadOrder, AddonConfiguration configuration) {
        AddonMeta meta = configuration.meta().get();
        String name = meta.name();
        if (loadOrder.contains(name))
            throw new IllegalStateException("There are two plugins with the same name: \"%s\"!".formatted(name));

        processDepends(name, meta.depends(), loadOrder::depends);
        processDepends(name, meta.softDepends(), loadOrder::softDepends);
    }

    /**
     * Injects the specified depends on using the given consumer.
     *
     * @param name     The name of the addon.
     * @param depends  The addon names which should be injected.
     * @param consumer The consumer which handles the injection.
     * @since 3.4.3
     */
    private void processDepends(String name, String[] depends, BiConsumer<String, String> consumer) {
        if (depends.length == 0) return;
        Arrays.stream(depends).distinct().forEach(depended -> consumer.accept(name, depended));
    }

    /**
     * Add an addon to the specified load order.
     *
     * @param loadOrder The load order in which the addon should be placed.
     * @param addon     The instance of the addon.
     * @since 3.4.3
     */
    private void addToLoadOrder(AddonLoadOrder loadOrder, Addon addon) {
        loadOrder.addAddon(addon);
    }

    /**
     * Add the code source of an addon to the specified code sources hash map
     *
     * @param configuration The configuration of the addon.
     * @param addon         The instance of the addon.
     * @param codesSources  The hash map which should store the code source.
     * @since 3.4.3
     */
    private void addCodeSource(AddonConfiguration configuration, Addon addon,
                               HashMap<URI, Map.Entry<JarFile, ArrayList<Addon>>> codesSources) {
        try {
            Path path;
            if (configuration.path() != null) path = configuration.path();
            else
                // Use the code source of the addon when it is not located in the addons folder
                path = Path.of(addon.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            URI uri = path.toUri();
            JarFile jarFile = craftsNet.getFileHelper().getJarFileAt(path);

            codesSources.computeIfAbsent(uri, f -> Map.entry(jarFile, new ArrayList<>())).getValue().add(addon);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Could not handle code source for addon %s!".formatted(
                    addon.getName()
            ), e);
        }
    }

    /**
     * Instantiates a new instance of the addon which is described via the
     * {@link AddonConfiguration configuration}.
     *
     * @param configuration The configuration containing the information about the addon.
     * @return A new instance of the addon.
     * @since 3.4.3
     */
    private Addon instantiateAddon(AddonConfiguration configuration) {
        try {
            AddonMeta meta = configuration.meta().get();

            String name = meta.name();
            Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
            if (!pattern.matcher(name).matches())
                throw new IllegalArgumentException("Plugin names must not contain special characters / spaces! Plugin name: \"" + name + "\"");

            logger.info("Found addon " + name + ", add it to load order");

            // Create addon class loader
            AddonClassLoader classLoader = configuration.classLoader().get();

            // Load the main class of the addon using the class loader
            String className = meta.mainClass();
            Class<?> clazz = className != null && !className.isBlank() ? classLoader.loadClass(className) : HollowAddon.class;
            if (clazz == null)
                throw new NullPointerException("The main class could not be found!");
            if (!Addon.class.isAssignableFrom(clazz))
                throw new IllegalArgumentException("The loaded main class (" + className +
                        ") is not an instance of " + Addon.class.getSimpleName() + "!");

            // Create an instance of the main class and inject dependencies using reflection
            Class<? extends Addon> addonClass = clazz.asSubclass(Addon.class);
            Addon addon = ReflectionUtils.getNewInstance(addonClass);
            ReflectionUtils.setField("craftsNet", addon, craftsNet);
            ReflectionUtils.setField("meta", addon, meta);
            ReflectionUtils.setField("logger", addon, logger.cloneWithName(name));
            ReflectionUtils.setField("classLoader", addon, classLoader);

            return addon;
        } catch (Exception e) {
            logger.error(e, "Could not load addon %s!".formatted(
                    configuration.meta().get().name()
            ));
        }
        return null;
    }

    /**
     * Converts all given code sources in the respective {@link AutoRegisterInfo auto register info}
     * which then can be used to perform an auto register action.
     *
     * @param loader       The {@link AutoRegisterLoader} which is used to load the infos.
     * @param codesSources The code sources which should be converted.
     * @return A hash map which contains all converted {@link AutoRegisterInfo infos}.
     * @since 3.4.3
     */
    private HashMap<Addon, List<AutoRegisterInfo>> convertToAutoRegister(AutoRegisterLoader loader,
                                                                         HashMap<URI, Map.Entry<JarFile, ArrayList<Addon>>> codesSources) {
        // ToDo: Fix that currently the first addon in the jar is loading all autoregister infos.

        final HashMap<Addon, List<AutoRegisterInfo>> autoRegisterInfos = new HashMap<>();
        codesSources.values().forEach((info) -> {
            final JarFile file = info.getKey();
            final ArrayList<Addon> bounding = info.getValue();

            // Skip if the jar file is null
            if (file == null) return;

            // Perform the actual load of the jar file if it is not null
            try (file) {
                bounding.forEach(addon -> {
                    try {
                        autoRegisterInfos.put(addon, loader.loadFrom(addon.getClassLoader(), bounding, file));
                    } catch (NoClassDefFoundError ignored) {
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Could not load auto register infos!", e);
            }
        });

        codesSources.clear();

        return autoRegisterInfos;
    }

    /**
     * Loads all the services from the {@link AddonConfiguration configuration}.
     *
     * @param configuration The {@link AddonConfiguration configuration} from which
     *                      the services are loaded.
     * @since 3.4.3
     */
    private void loadServices(AddonConfiguration configuration) {
        ServiceManager serviceManager = craftsNet.getServiceManager();

        if (configuration.services() == null || configuration.services().isEmpty()) return;
        if (configuration.addon().get() == null) return;
        if (!(configuration.addon().get().getClassLoader() instanceof AddonClassLoader classLoader)) return;

        configuration.services().forEach(service -> {
            for (String provider : service.provider().split(";"))
                try {
                    Class<?> spi = classLoader.loadClass(service.spi());
                    Class<?> providerClass = classLoader.loadClass(provider);
                    if (serviceManager.load(spi, providerClass))
                        logger.debug("Registered service " + provider + " for " + spi.getName());
                    else
                        logger.debug("No service loader found for service " + spi.getName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Could not register service %s for %s!".formatted(
                            provider, configuration.addon().get().getName()
                    ), e);
                }
        });
    }

    /**
     * Checks if a specific {@link JarFile} is compatible with the current version of the jvm.
     *
     * @param file The {@link JarFile} which should be checked.
     * @throws RuntimeException If the jvm version is not compatible with the {@link JarFile}.
     */
    private void compatibleOrThrow(final JarFile file) {
        final int maxMajor = Runtime.version().feature() + 44;
        final int maxMinor = Runtime.version().interim();

        file.stream().filter(jarEntry -> jarEntry.getName().endsWith(".class") && !jarEntry.isDirectory())
                .forEach(jarEntry -> {
                    if (!jarEntry.getName().endsWith(".class")) return;

                    try (DataInputStream dis = new DataInputStream(file.getInputStream(jarEntry))) {
                        if (dis.readInt() != 0xCAFEBABE)
                            throw new RuntimeException(jarEntry.getName() + " is not an valid class file! There was an magic number mismatch with" +
                                    "the first 4 bytes!");

                        int minor = dis.readUnsignedShort();
                        int major = dis.readUnsignedShort();

                        if (major > maxMajor || (major == maxMajor && minor > maxMinor))
                            throw new RuntimeException(jarEntry.getName().replace(".class", "") + " " +
                                    "has been compiled by a more recent version of the Java Runtime (class file version " + major + "." + minor + "), " +
                                    "this version of the Java Runtime only recognizes class file versions up to " + maxMajor + "." + maxMinor);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Loads the addon jar file and extracts the configuration.
     *
     * @param source The source {@link Path} from which the addon is loaded.
     * @param file   The addon {@link JarFile} to load.
     * @return The addon configuration if found, otherwise null.
     * @throws IOException if there is an I/O error while loading the jar file.
     */
    private AddonConfiguration retrieveConfig(Path source, JarFile file) throws IOException {
        AddonConfiguration configuration;
        JarEntry entry = file.getJarEntry("addon.json");
        if (entry == null) return null;

        // Read the configuration data
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(entry)))) {
            StringBuilder json = new StringBuilder();
            reader.lines().forEach(json::append);
            configuration = AddonConfiguration.of(source, JsonParser.parse(json.toString()), null, null, new ConcurrentLinkedQueue<>());
        }

        // Load the services from the path
        configuration.services().addAll(retrieveServices(file));
        return configuration;
    }

    /**
     * Utility method for loading RegistrableService instances from a jar file.
     *
     * @param file The jar file from which to load RegistrableService instances.
     * @return A list of RegistrableService instances loaded from the specified jar file.
     * @throws IOException If an I/O error occurs while processing the jar file or reading its contents.
     * @see RegisteredService
     */
    public List<RegisteredService> retrieveServices(JarFile file) throws IOException {
        List<RegisteredService> services = new ArrayList<>();

        // Iterate over all entries in the jar file
        Iterator<JarEntry> iterator = file.stream().iterator();
        while (iterator.hasNext()) {
            JarEntry entry = iterator.next();

            // Check whether the entry is in the "META-INF/services" directory and not a directory
            if (!entry.getName().trim().toLowerCase().startsWith("meta-inf/services") || entry.isDirectory())
                continue;

            // Read the contents of the file in the jar entry
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(entry)))) {
                StringBuilder provider = new StringBuilder();

                // Process each line in the file
                reader.lines().forEach(line -> {
                    // Ignore comments and empty lines
                    if (line.trim().startsWith("#") || line.trim().startsWith("//") || line.isBlank()) return;

                    // Add the provider to the list
                    if (!provider.toString().trim().isBlank()) provider.append(";");
                    provider.append(line);
                });

                // Extract the name from the path and create a RegistrableService object
                String[] splitName = entry.getName().split("/");
                services.add(new RegisteredService(splitName[splitName.length - 1], provider.toString()));
            }
        }

        return services;
    }

}
