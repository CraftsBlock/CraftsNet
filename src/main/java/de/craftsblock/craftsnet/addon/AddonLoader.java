package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.actions.CompleteAbleActionImpl;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * The AddonLoader class is responsible for loading and managing addons in the application.
 * It loads addon JAR files, extracts necessary information, and initializes addon instances.
 *
 * @author CraftsBlock
 * @version 1.0.2
 * @see Addon
 * @see AddonManager
 * @since 1.0.0
 */
public class AddonLoader {

    /**
     * TODO: Add a way to define a load order of the plugins / addons
     */

    private URLClassLoader classLoader;
    private final Stack<File> addons = new Stack<>();

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
    protected void add(File file) {
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
        Logger logger = CraftsNet.logger();
        logger.info("All addons are loaded");

        // Load all the dependencies and repositories from the addons
        ConcurrentHashMap<File, Configuration> configurations = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<URL> urls = new ConcurrentLinkedQueue<>();
        for (File file : addons) {
            Configuration configuration = loadConfig(file);
            Json addon = configuration.json();
            String name = addon.getString("name");

            if (addon.contains("repositories"))
                for (String repo : addon.getStringList("repositories"))
                    ArtifactLoader.addRepository(repo);

            URL[] dependencies = new URL[0];
            if (addon.contains("dependencies")) {
                dependencies = ArtifactLoader.loadLibraries(this, configuration, name, addon.getStringList("dependencies").toArray(String[]::new));
                urls.addAll(Arrays.asList(dependencies));
            }

            configurations.put(file, new Configuration(configuration.json(), dependencies, configuration.services));
        }

        // Create an array of URLs containing the addon file locations
        for (File file : addons)
            urls.add(file.toURI().toURL());
        classLoader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getSystemClassLoader());

        ConcurrentHashMap<String, CompleteAbleActionImpl<Void>> tasks = new ConcurrentHashMap<>();

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

                String name = configuration.json.getString("name");
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
                if (!pattern.matcher(name).matches())
                    throw new IllegalArgumentException("Plugin names must not contain special characters / spaces! Plugin name: \"" + name + "\"");
                if (tasks.containsKey(name))
                    throw new IllegalStateException("There are two plugins with the same name: \"" + name + "\"!");

                logger.debug("Addon " + name + " is loaded");

                // Load the main class of the addon using the class loader
                String className = configuration.json.getString("main");
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz == null)
                    throw new NullPointerException("The main class could not be found!");
                if (!Addon.class.isAssignableFrom(clazz))
                    throw new IllegalArgumentException("The loaded main class (" + className + ") is not an instance of Addon!");

                // Create an instance of the main class and inject dependencies using reflection
                Addon obj = (Addon) clazz.getDeclaredConstructor().newInstance();
                setField("commandRegistry", obj, CraftsNet.commandRegistry());
                setField("handler", obj, CraftsNet.routeRegistry());
                setField("listenerRegistry", obj, CraftsNet.listenerRegistry());
                setField("logger", obj, logger);
                setField("name", obj, name);
                setField("serviceManager", obj, CraftsNet.serviceManager());

                tasks.put(name.toLowerCase(), new CompleteAbleActionImpl<>(() -> {
                    Json addon = configuration.json;
                    obj.onLoad();

                    if (addon.contains("depends"))
                        for (String depended : addon.getStringList("depends"))
                            if (tasks.containsKey(depended)) tasks.remove(depended).complete();

                    // Call the 'onEnable' method of the addon instance asynchronously
                    obj.onEnable();
                    manager.register(obj);
                    return null;
                }));
            } catch (Exception e) {
                logger.error(e);
            }

        // Complete the 'onEnable' method calls for all addons
        tasks.keySet().forEach(name -> tasks.remove(name).complete());
        tasks.clear();
        if (addons.isEmpty())
            logger.debug("No addons found to load");
        logger.info("All addons were loaded within " + (System.currentTimeMillis() - start) + "ms");
        addons.clear();

        // Load all the registrable services
        ServiceManager serviceManager = CraftsNet.serviceManager();
        for (Configuration configuration : configurations.values()) {
            if (configuration.services() != null && !configuration.services().isEmpty())
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
        configurations.clear();
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
    private Configuration loadConfig(File file) throws IOException {
        if (file.isDirectory())
            return new Configuration(JsonParser.parse("{\"isfolder\":[]}"), null, Collections.emptyList());
        Configuration configuration;
        try (JarFile jarFile = new JarFile(file)) {
            JarEntry entry = jarFile.getJarEntry("addon.json");
            if (entry == null) return null;

            // Read the configuration data
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))) {
                StringBuilder json = new StringBuilder();
                reader.lines().forEach(json::append);
                configuration = new Configuration(JsonParser.parse(json.toString()), null, new ConcurrentLinkedQueue<>());
            }

            // Load the services from the file
            configuration.services().addAll(loadServices(file));
        }
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
    protected List<RegistrableService> loadServices(File file) throws IOException {
        List<RegistrableService> services = new ArrayList<>();

        // Check whether the directory is empty
        if (file.isDirectory()) return services;

        try (JarFile jar = new JarFile(file)) {
            // Iterate over all entries in the JAR file
            Iterator<JarEntry> iterator = jar.stream().iterator();
            while (iterator.hasNext()) {
                JarEntry entry = iterator.next();

                // Check whether the entry is in the "META-INF/services" directory and not a directory
                if (!entry.getName().trim().toLowerCase().startsWith("meta-inf/services") || entry.isDirectory())
                    continue;

                // Read the contents of the file in the JAR entry
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)))) {
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
     */
    protected record Configuration(Json json, URL[] classpath, Collection<RegistrableService> services) {
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
