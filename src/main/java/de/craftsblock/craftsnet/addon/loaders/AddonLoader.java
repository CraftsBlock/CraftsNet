package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
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

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * The AddonLoader class is responsible for loading and managing addons in the application.
 * It loads addon JAR files, extracts necessary information, and initializes addon instances.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.1.8
 * @see Addon
 * @see AddonManager
 * @since 1.0.0-SNAPSHOT
 */
public final class AddonLoader {

    private final Stack<File> addons = new Stack<>();
    private final CraftsNet craftsNet;
    private final Logger logger;

    /**
     * Constructs a new instance of an addon loader
     *
     * @param craftsNet The CraftsNet instance which instantiates this
     */
    public AddonLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
        this.logger = this.craftsNet.logger();
    }

    /**
     * Adds a new addon file to the loader using the file name.
     *
     * @param file The name of the addon file.
     */
    public void add(String file) {
        add(new File("./addons/", file));
    }

    /**
     * Adds a new addon file to the loader using the file object.
     *
     * @param file The addon file to add.
     */
    public void add(File file) {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) throw new NullPointerException("The file (" + file.getPath() + ") does not exist!");
        if (!addons.contains(file))
            addons.push(file);
    }

    /**
     * Loads all the addons from the provided addon files.
     *
     * @throws IOException if there is an I/O error while loading the addons.
     */
    public void load() throws IOException {
        long start = System.currentTimeMillis();
        Logger logger = craftsNet.logger();
        logger.info("Load all available addons");

        // Create a new artifact loader
        ArtifactLoader artifactLoader = new ArtifactLoader();

        // Load all the dependencies and repositories from the addons
        List<AddonConfiguration> configurations = new ArrayList<>();
        for (File file : addons) {
            if (file.isDirectory()) continue;

            try (JarFile jarFile = new JarFile(file, true, ZipFile.OPEN_READ, Runtime.version())) {
                logger.debug("Loading jar file " + file.getAbsolutePath());

                // Load the configuration file from the jar
                AddonConfiguration configuration = loadConfig(file, jarFile);
                if (configuration == null) {
                    logger.error(new FileNotFoundException("Could not locate the addon.json within " + file.getPath() + "!"));
                    continue;
                }
                Json addon = configuration.json();
                String name = addon.getString("name");

                // Check if the jar version is compatible
                long checkStart = System.currentTimeMillis();

                try {
                    compatibleOrThrow(jarFile);
                    logger.debug(file.getAbsolutePath() + " is jvm compatible, checked within " + (System.currentTimeMillis() - checkStart) + "ms");
                } catch (RuntimeException e) {
                    logger.error(e);
                    logger.error(file.getAbsolutePath() + " is not jvm compatible, checked within " + (System.currentTimeMillis() - checkStart) + "ms");
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
                    dependencies = artifactLoader.loadLibraries(this.craftsNet, this, configuration.services(), name, addon.getStringList("dependencies").toArray(String[]::new));
                else dependencies = null;

                // Generate classpath
                URL[] classpath;
                if (dependencies != null)
                    classpath = Stream.concat(Arrays.stream(dependencies), Stream.of(file.toURI().toURL())).toArray(URL[]::new);
                else classpath = new URL[]{file.toURI().toURL()};

                // Put the configuration in the configurations map
                configurations.add(new AddonConfiguration(file, configuration.json(), classpath, configuration.services(), configuration.addon(), configuration.meta()));
            }
        }
        artifactLoader.stop();

        load(configurations);
        configurations.clear();

        if (addons.isEmpty()) logger.info("No addons found to load");
        else logger.info("All addons were loaded within " + (System.currentTimeMillis() - start) + "ms");
        addons.clear();

        try {
            craftsNet.listenerRegistry().call(new AllAddonsLoadedEvent());
        } catch (Exception e) {
            logger.error(e, "Can not fire addons loaded event!");
        }
    }

    /**
     * Loads all the addons from the provided list of {@link AddonConfiguration}.
     *
     * @param configurations The list of {@link AddonConfiguration}.
     */
    public void load(List<AddonConfiguration> configurations) {
        AddonLoadOrder loadOrder = new AddonLoadOrder();
        AutoRegisterLoader autoRegisterLoader = new AutoRegisterLoader();
        HashMap<Addon, List<AutoRegisterInfo>> autoRegisterInfos = new HashMap<>();

        for (AddonConfiguration configuration : configurations)
            try {
                AddonMeta meta = AddonMeta.of(configuration);

                String name = meta.name();
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
                if (!pattern.matcher(name).matches())
                    throw new IllegalArgumentException("Plugin names must not contain special characters / spaces! Plugin name: \"" + name + "\"");
                if (loadOrder.contains(name))
                    throw new IllegalStateException("There are two plugins with the same name: \"" + name + "\"!");

                logger.info("Found addon " + name + ", add it to load order");

                // Create addon class loader
                AddonClassLoader classLoader = new AddonClassLoader(this.craftsNet, configuration);

                // Load the main class of the addon using the class loader
                String className = meta.mainClass();
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz == null)
                    throw new NullPointerException("The main class could not be found!");
                if (!Addon.class.isAssignableFrom(clazz))
                    throw new IllegalArgumentException("The loaded main class (" + className +
                            ") is not an instance of " + Addon.class.getSimpleName() + "!");

                // Create an instance of the main class and inject dependencies using reflection
                Addon obj = clazz.asSubclass(Addon.class).getDeclaredConstructor().newInstance();
                ReflectionUtils.setField("craftsNet", obj, craftsNet);
                ReflectionUtils.setField("meta", obj, meta);
                ReflectionUtils.setField("logger", obj, logger.cloneWithName(name));
                ReflectionUtils.setField("classLoader", obj, classLoader);

                loadOrder.addAddon(obj);
                Json addon = configuration.json();
                if (addon.contains("depends"))
                    for (String depended : addon.getStringList("depends"))
                        loadOrder.depends(obj, depended);

                if (addon.contains("softDepends"))
                    for (String depended : addon.getStringList("softDepends"))
                        loadOrder.softDepends(obj, depended);

                craftsNet.addonManager().register(obj);
                configuration.addon().set(obj);

                // Load the file the addon is located in
                JarFile file;
                if (configuration.file() != null) file = new JarFile(configuration.file());
                else {
                    // Use the code source of the addon when it is not located in the addons folder
                    Path path = Path.of(obj.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                    file = craftsNet.fileHelper().getJarFileAt(path);
                }

                // Perform the actual load of the jar file if it is not null
                if (file != null)
                    try (file) {
                        autoRegisterInfos.put(obj, autoRegisterLoader.loadFrom(classLoader, obj, file));
                    }
            } catch (Exception e) {
                logger.error(e);
            }

        // Loading all addons
        Collection<Addon> orderedLoad = loadOrder.getLoadOrder();
        for (Addon addon : orderedLoad) {
            logger.info("Loading addon " + addon.getName() + "...");
            addon.onLoad();

            if (!autoRegisterInfos.containsKey(addon)) break;
            craftsNet.autoRegisterRegistry().handleAll(autoRegisterInfos.get(addon), Startup.LOAD);
        }

        // Enabling all addons
        for (Addon addon : orderedLoad) {
            logger.info("Enabling addon " + addon.getName() + "...");
            addon.onEnable();

            if (!autoRegisterInfos.containsKey(addon)) break;
            craftsNet.autoRegisterRegistry().handleAll(autoRegisterInfos.get(addon), Startup.ENABLE);
        }

        // Load all the registrable services
        ServiceManager serviceManager = craftsNet.serviceManager();
        for (AddonConfiguration configuration : configurations) {
            if (configuration.services() != null && !configuration.services().isEmpty() && configuration.addon().get() != null) {
                if (!(configuration.addon().get().getClassLoader() instanceof AddonClassLoader classLoader)) continue;
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
                            throw new RuntimeException(e);
                        }
                });
            }
        }
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

                        if (major > maxMajor || minor > maxMinor)
                            throw new RuntimeException(jarEntry.getName().replace(".class", "") + " " +
                                    "has been compiled by a more recent version of the Java Runtime (class file version " + major + "." + minor + "), " +
                                    "this version of the Java Runtime only recognizes class file versions up to " + maxMajor + "." + maxMinor);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Loads the addon JAR file and extracts the configuration.
     *
     * @param source The source {@link File} from which the addon is loaded.
     * @param file   The addon {@link JarFile} to load.
     * @return The addon configuration if found, otherwise null.
     * @throws IOException if there is an I/O error while loading the JAR file.
     */
    private AddonConfiguration loadConfig(File source, JarFile file) throws IOException {
        AddonConfiguration configuration;
        JarEntry entry = file.getJarEntry("addon.json");
        if (entry == null) return null;

        // Read the configuration data
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(entry)))) {
            StringBuilder json = new StringBuilder();
            reader.lines().forEach(json::append);
            configuration = AddonConfiguration.of(source, JsonParser.parse(json.toString()), null, new ConcurrentLinkedQueue<>());
        }

        // Load the services from the file
        configuration.services().addAll(loadServices(file));
        return configuration;
    }

    /**
     * Utility method for loading RegistrableService instances from a JAR file.
     *
     * @param file The JAR file from which to load RegistrableService instances.
     * @return A list of RegistrableService instances loaded from the specified JAR file.
     * @throws IOException If an I/O error occurs while processing the JAR file or reading its contents.
     * @see RegisteredService
     */
    List<RegisteredService> loadServices(JarFile file) throws IOException {
        List<RegisteredService> services = new ArrayList<>();

        // Iterate over all entries in the JAR file
        Iterator<JarEntry> iterator = file.stream().iterator();
        while (iterator.hasNext()) {
            JarEntry entry = iterator.next();

            // Check whether the entry is in the "META-INF/services" directory and not a directory
            if (!entry.getName().trim().toLowerCase().startsWith("meta-inf/services") || entry.isDirectory())
                continue;

            // Read the contents of the file in the JAR entry
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
