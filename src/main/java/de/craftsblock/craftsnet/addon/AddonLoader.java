package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.actions.CompleteAbleActionImpl;
import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.Main;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonLoader {

    private URLClassLoader classLoader;
    private final Stack<File> addons = new Stack<>();

    public void add(String file) {
        add(new File("./addons/", file));
    }

    protected void add(File file) {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) throw new NullPointerException("Datei (" + file.getPath() + ") existiert nicht!");
        if (!addons.contains(file))
            addons.push(file);
    }

    public void load() throws IOException {
        long start = System.currentTimeMillis();
        Logger logger = Main.logger;
        logger.info("Addons werden geladen");
        URL[] urls = new URL[addons.size()];
        int curr = 0;
        for (File file : addons)
            urls[curr++] = file.toURI().toURL();
        classLoader = new URLClassLoader(urls, getClass().getClassLoader());
        ConcurrentLinkedQueue<CompleteAbleActionImpl<Void>> tasks = new ConcurrentLinkedQueue<>();
        for (File file : addons)
            try {
                Configuration configuration = loadJar(file);
                if (configuration == null)
                    throw new NullPointerException("Das Plugin " + file.getName() + " beinhaltet kein addon.json");
                if (configuration.json.contains("isfolder"))
                    continue;
                String name = configuration.json.getString("name");
                logger.debug("Addon " + name + " wird geladen");
                String className = configuration.json.getString("main");
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz == null)
                    throw new NullPointerException("Die Main Klasse konnte nicht gefunden werden!");
                if (!Addon.class.isAssignableFrom(clazz))
                    throw new IllegalStateException("Die zuladene Main Klasse (" + className + ") ist keine instance von Addon!");
                Object obj = clazz.getDeclaredConstructor().newInstance();
                setField("name", obj, name);
                setField("logger", obj, logger);
                setField("handler", obj, Main.routeRegistry);
                setField("registry", obj, Main.listenerRegistry);
                obj.getClass().getMethod("onLoad").invoke(obj);
                tasks.add(new CompleteAbleActionImpl<>(() -> {
                    obj.getClass().getMethod("onEnable").invoke(obj);
                    return null;
                }));
            } catch (Exception e) {
                logger.error(e);
            }
        tasks.forEach(CompleteAbleActionImpl::complete);
        if (addons.isEmpty())
            logger.debug("Keine Addons zum laden gefunden");
        logger.info("Alle Addons wurde innerhalb von " + (System.currentTimeMillis() - start) + "ms geladen");
    }

    private void setField(String name, Object obj, Object arg) throws IllegalAccessException {
        Field field = getField(obj.getClass(), name);
        field.setAccessible(true);
        field.set(obj, arg);
        field.setAccessible(false);
    }

    public Configuration loadJar(File file) throws IOException, ClassNotFoundException {
        Configuration configuration = null;
        if (file.isDirectory())
            return new Configuration(JsonParser.parse("{\"isfolder\":[]}"));
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                    Class<?> clazz = classLoader.loadClass(className);
                    continue;
                }

                if (entryName.equalsIgnoreCase("addon.json")) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))) {
                        configuration = new Configuration(JsonParser.parse(reader.readLine()));
                    }
                }
            }
        }
        return configuration;
    }

    private Field getField(Class<?> clazz, String name) {
        Field field = null;
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name);
            } catch (Exception ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return field;
    }

    public record Configuration(Json json) {
    }

}
