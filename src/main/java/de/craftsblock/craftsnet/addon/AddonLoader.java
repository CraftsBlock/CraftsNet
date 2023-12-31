package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.actions.CompleteAbleActionImpl;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Stack;
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
 * @see Addon
 * @see AddonManager
 * @since 1.0.0
 */
public class AddonLoader {

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
        Logger logger = CraftsNet.logger;
        logger.info("Addons werden geladen");

        // Create an array of URLs containing the addon file locations
        URL[] urls = new URL[addons.size()];
        int curr = 0;
        for (File file : addons)
            urls[curr++] = file.toURI().toURL();
        classLoader = new URLClassLoader(urls, getClass().getClassLoader());
        ConcurrentHashMap<String, CompleteAbleActionImpl<Void>> tasks = new ConcurrentHashMap<>();

        // Loop through each addon file and load its configuration and main class
        for (File file : addons)
            try {
                // Load the addon's configuration from the addon JAR
                Configuration configuration = loadJar(file);
                if (configuration == null)
                    throw new NullPointerException("Das Plugin " + file.getName() + " beinhaltet kein addon.json");
                if (configuration.json.contains("isfolder"))
                    continue;
                String name = configuration.json.getString("name");
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9]$");
                if (!pattern.matcher(name).matches())
                    throw new IllegalArgumentException("Plugin names must not contain special characters / spaces! Plugin name: \"" + name + "\"");
                if (tasks.containsKey(name))
                    throw new IllegalStateException("There are two plugins with the same name: \"" + name + "\"!");
                logger.debug("Addon " + name + " wird geladen");

                // Load the main class of the addon using the class loader
                String className = configuration.json.getString("main");
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz == null)
                    throw new NullPointerException("The main class could not be found!");
                if (!Addon.class.isAssignableFrom(clazz))
                    throw new IllegalArgumentException("The loaded main class (" + className + ") is not an instance of Addon!");

                // Create an instance of the main class and inject dependencies using reflection
                Addon obj = (Addon) clazz.getDeclaredConstructor().newInstance();
                setField("name", obj, name);
                setField("logger", obj, logger);
                setField("handler", obj, CraftsNet.routeRegistry);
                setField("listenerRegistry", obj, CraftsNet.listenerRegistry);
                setField("commandRegistry", obj, CraftsNet.commandRegistry);

                tasks.put(name.toLowerCase(), new CompleteAbleActionImpl<>(() -> {
                    Json addon = configuration.json;
//                    if(addon.contains("loadbefore"))
                    // Call the 'onLoad' method of the addon instance
                    obj.onLoad();

                    if (addon.contains("depends"))
                        for (String depended : addon.getStringList("depends"))
                            if (tasks.containsKey(depended)) tasks.remove(depended).complete();

                    // Call the 'onEnable' method of the addon instance asynchronously
                    obj.onEnable();
                    manager.register(obj);
//                    obj.getClass().getMethod("onEnable").invoke(obj);
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
    public Configuration loadJar(File file) throws IOException {
        Configuration configuration = null;
        if (file.isDirectory())
            return new Configuration(JsonParser.parse("{\"isfolder\":[]}"));
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            // Loop through each entry in the addon JAR
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Check if the entry is a class file and attempt to load the class
                if (entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                    try {
                        classLoader.loadClass(className);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        CraftsNet.logger.error("The class " + className + " could not be found");
                        continue;
                    }
                    continue;
                }

                // Check if the entry is 'addon.json' and read the configuration data
                if (entryName.equalsIgnoreCase("addon.json")) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))) {
                        StringBuilder json = new StringBuilder();
                        reader.lines().forEach(json::append);
                        configuration = new Configuration(JsonParser.parse(json.toString()));
                    }
                }
            }
        }
        return configuration;
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
     */
    public record Configuration(Json json) {
    }

}
