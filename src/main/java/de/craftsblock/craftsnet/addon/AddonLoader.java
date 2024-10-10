package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.events.addons.AddonsLoadedEvent;
import de.craftsblock.craftsnet.logging.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * The AddonLoader class is responsible for loading and managing addons in the application.
 * It loads addon JAR files, extracts necessary information, and initializes addon instances.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.3
 * @see Addon
 * @see AddonManager
 * @since 1.0.0-SNAPSHOT
 */
final class AddonLoader {

    private final Stack<File> addons = new Stack<>();
    private final CraftsNet craftsNet;

    /**
     * Constructs a new instance of an addon loader
     *
     * @param craftsNet The CraftsNet instance which instantiates this
     */
    public AddonLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
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
    void add(File file) {
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
    public void load(AddonManager manager) throws IOException {
        long start = System.currentTimeMillis();
        Logger logger = craftsNet.logger();
        logger.info("Load all available addons");

        // Create a new artifact loader
        ArtifactLoader artifactLoader = new ArtifactLoader();

        // Load all the dependencies and repositories from the addons
        ConcurrentHashMap<File, Configuration> configurations = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ConcurrentLinkedQueue<URL>> urls = new ConcurrentHashMap<>();
        for (File file : addons) {
            if (file.isDirectory()) {
                logger.warning("");
                continue;
            }

            try (JarFile jarFile = new JarFile(file, true, ZipFile.OPEN_READ, Runtime.version())) {
                logger.debug("Loading jar file " + file.getAbsolutePath());

                // Load the configuration file from the jar
                Configuration configuration = loadConfig(jarFile);
                if (configuration == null) {
                    logger.error(new FileNotFoundException("Could not locate the addon.json within " + file.getPath() + "!"));
                    continue;
                }
                Json addon = configuration.json();
                String name = addon.getString("name");

                // Check if the jar version is compatible
                long checkStart = System.currentTimeMillis();

                int maxMajor = Runtime.version().feature() + 44;
                int maxMinor = Runtime.version().interim();
                AtomicBoolean skip = new AtomicBoolean(false);
                jarFile.stream().filter(jarEntry -> jarEntry.getName().endsWith(".class") && !jarEntry.isDirectory())
                        .forEach(jarEntry -> {
                            if (!jarEntry.getName().endsWith(".class")) return;
                            if (skip.get()) return;
                            try (DataInputStream dis = new DataInputStream(jarFile.getInputStream(jarEntry))) {
                                if (dis.readInt() != 0xCAFEBABE) {
                                    skip.set(true);
                                    logger.error("Error loading addon " + name + " (" + file.getName() + "), skipping!");
                                    logger.error(new RuntimeException(
                                            jarEntry.getName() + " is not an valid class file! There was an magic number mismatch with the first 4 bytes!"
                                    ));
                                    return;
                                }

                                int minor = dis.readUnsignedShort();
                                int major = dis.readUnsignedShort();
                                skip.set(major > maxMajor || minor > maxMinor);
                                if (skip.get()) {
                                    logger.error("Error loading addon " + name + " (" + file.getName() + "), skipping!");
                                    logger.error(new RuntimeException(
                                            jarEntry.getName().replace(".class", "") + " " +
                                                    "has been compiled by a more recent version of the Java Runtime (class file version " + major + "." + minor + "), " +
                                                    "this version of the Java Runtime only recognizes class file versions up to " + maxMajor + "." + maxMinor)
                                    );
                                }
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        });
                if (skip.get()) {
                    logger.error(file.getAbsolutePath() + " is not jvm compatible, checked within " + (System.currentTimeMillis() - checkStart) + "ms");
                    continue;
                }

                logger.debug(file.getAbsolutePath() + " is jvm compatible, checked within " + (System.currentTimeMillis() - checkStart) + "ms");

                // Inject all repositories
                artifactLoader.cleanup();
                if (addon.contains("repositories"))
                    for (String repo : addon.getStringList("repositories"))
                        artifactLoader.addRepository(repo);

                // Load all required dependencies
                URL[] dependencies = new URL[0];
                if (addon.contains("dependencies")) {
                    dependencies = artifactLoader.loadLibraries(this.craftsNet, this, configuration, name, addon.getStringList("dependencies").toArray(String[]::new));
                    urls.computeIfAbsent(name, s -> new ConcurrentLinkedQueue<>()).addAll(Arrays.asList(dependencies));
                }

                // Put the configuration in the configurations map
                configurations.put(file, new Configuration(configuration.json(), dependencies, configuration.services, configuration.addon(), configuration.meta));
            }
        }
        artifactLoader.stop();

        AddonLoadOrder loadOrder = new AddonLoadOrder();

        // Loop through each addon file and load its configuration and main class
        for (File file : addons)
            try {
                // Load the addon's configuration from the addon JAR
                if (!configurations.containsKey(file)) continue;
                Configuration configuration = configurations.get(file);

                if (configuration == null)
                    throw new NullPointerException("The Plugin " + file.getName() + " does not contains a addon.json");
                if (configuration.json.contains("isfolder"))
                    continue;

                AddonMeta meta = AddonMeta.of(configuration);

                String name = meta.name();
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
                if (!pattern.matcher(name).matches())
                    throw new IllegalArgumentException("Plugin names must not contain special characters / spaces! Plugin name: \"" + name + "\"");
                if (loadOrder.contains(name))
                    throw new IllegalStateException("There are two plugins with the same name: \"" + name + "\"!");

                logger.info("Found addon " + name + ", add it to load order");

                // Create addon class loader
                ConcurrentLinkedQueue<URL> addonUrls = urls.getOrDefault(name, new ConcurrentLinkedQueue<>());
                addonUrls.add(file.toURI().toURL());
                AddonClassLoader classLoader = new AddonClassLoader(this.craftsNet, manager, configuration, addonUrls.toArray(URL[]::new));

                // Load the main class of the addon using the class loader
                String className = meta.mainClass();
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz == null)
                    throw new NullPointerException("The main class could not be found!");
                if (!Addon.class.isAssignableFrom(clazz))
                    throw new IllegalArgumentException("The loaded main class (" + className + ") is not an instance of Addon!");

                // Create an instance of the main class and inject dependencies using reflection
                Addon obj = (Addon) clazz.getDeclaredConstructor().newInstance();
                setField("craftsNet", obj, craftsNet);
                setField("meta", obj, meta);
                setField("bodyRegistry", obj, craftsNet.bodyRegistry());
                setField("commandRegistry", obj, craftsNet.commandRegistry());
                setField("routeRegistry", obj, craftsNet.routeRegistry());
                setField("listenerRegistry", obj, craftsNet.listenerRegistry());
                setField("logger", obj, logger.cloneWithName(name));
                setField("name", obj, name);
                setField("classLoader", obj, classLoader);
                setField("serviceManager", obj, craftsNet.serviceManager());

                loadOrder.addAddon(obj);
                Json addon = configuration.json;
                if (addon.contains("depends"))
                    for (String depended : addon.getStringList("depends"))
                        loadOrder.depends(obj, depended);
                manager.register(obj);
                configuration.addon.set(obj);
            } catch (Exception e) {
                logger.error(e);
            }

        // Loading all addons
        Collection<Addon> orderedLoad = loadOrder.getLoadOrder();
        for (Addon addon : orderedLoad) {
            logger.info("Loading addon " + addon.getName() + "...");
            addon.onLoad();
        }

        // Enabling all addons
        for (Addon addon : orderedLoad) {
            logger.info("Enabling addon " + addon.getName() + "...");
            addon.onEnable();
        }

        if (addons.isEmpty()) logger.info("No addons found to load");
        else logger.info("All addons were loaded within " + (System.currentTimeMillis() - start) + "ms");
        addons.clear();

        // Load all the registrable services
        ServiceManager serviceManager = craftsNet.serviceManager();
        for (Configuration configuration : configurations.values()) {
            if (configuration.services() != null && !configuration.services().isEmpty() && configuration.addon().get() != null) {
                if (!(configuration.addon().get().getClassLoader() instanceof AddonClassLoader classLoader)) continue;
                configuration.services().forEach(service -> {
                    for (String provider : service.provider.split(";"))
                        try {
                            Class<?> spi = classLoader.loadClass(service.spi);
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
        configurations.clear();

        try {
            craftsNet.listenerRegistry().call(new AddonsLoadedEvent());
        } catch (Exception e) {
            logger.error(e, "Can not fire addons loaded event!");
        }
    }

    /**
     * Sets a field in the given object using reflection.
     *
     * @param name The name of the field to set.
     * @param obj  The object whose field needs to be set.
     * @param arg  The value to set the field to.
     * @throws IllegalAccessException if the field cannot be accessed.
     */
    private void setField(String name, Object obj, Object arg) throws IllegalAccessException {
        Field field = getField(obj.getClass(), name); // Find the specified field in the object's class hierarchy using reflection
        field.setAccessible(true); // Set the fields accessibility to true
        field.set(obj, arg); // Set the field value to the provided argument
        field.setAccessible(false); // Set the fields accessibility to false
    }

    /**
     * Loads the addon JAR file and extracts the configuration.
     *
     * @param file The addon JAR file to load.
     * @return The addon configuration if found, otherwise null.
     * @throws IOException if there is an I/O error while loading the JAR file.
     */
    private Configuration loadConfig(JarFile file) throws IOException {
        Configuration configuration;
        JarEntry entry = file.getJarEntry("addon.json");
        if (entry == null) return null;

        // Read the configuration data
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(entry)))) {
            StringBuilder json = new StringBuilder();
            reader.lines().forEach(json::append);
            configuration = Configuration.of(JsonParser.parse(json.toString()), null, new ConcurrentLinkedQueue<>());
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
     * @see RegistrableService
     */
    List<RegistrableService> loadServices(JarFile file) throws IOException {
        List<RegistrableService> services = new ArrayList<>();

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
                services.add(new RegistrableService(splitName[splitName.length - 1], provider.toString()));
            }
        }

        return services;
    }


    /**
     * Finds a field with the given name in the specified class, including inherited classes.
     *
     * @param clazz The class to search for the field.
     * @param name  The name of the field to find.
     * @return The field if found, otherwise null.
     */
    private Field getField(Class<?> clazz, String name) {
        Field field = null;
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name); // Attempt to get the specified field from the current class
            } catch (Exception ignored) {
            }
            clazz = clazz.getSuperclass(); // Move to the superclass for further field search
        }
        return field;
    }

    /**
     * The Configuration class represents the configuration for an addon.
     * It holds the configuration JSON data.
     *
     * @param json      Content of the addon.json
     * @param classpath Classpath of the jar file
     * @param services  Services that should be registered
     * @param addon     The loaded addon instance
     * @param meta      The metadata of the addon
     */
    protected record Configuration(Json json, URL[] classpath, Collection<RegistrableService> services, AtomicReference<Addon> addon,
                                   AtomicReference<AddonMeta> meta) {

        /**
         * Creates an {@link Configuration} instance from the provided params.
         *
         * @param json      Content of the addon.json
         * @param classpath Classpath of the jar file
         * @param services  Services that should be registered
         * @return A new instance of {@link Configuration}.
         * @since 3.0.7-SNAPSHOT
         */
        private static Configuration of(Json json, URL[] classpath, Collection<RegistrableService> services) {
            return new Configuration(json, classpath, services, new AtomicReference<>(), new AtomicReference<>());
        }

    }

    /**
     * Represents a registrable service, encapsulating the service provider interface (SPI)
     * and the provider information in a concise record.
     *
     * @param spi      The name of the service provider interface (SPI).
     * @param provider Information about the service provider.
     */
    protected record RegistrableService(String spi, String provider) {
    }

}
