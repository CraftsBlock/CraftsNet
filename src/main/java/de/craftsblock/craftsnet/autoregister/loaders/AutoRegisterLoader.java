package de.craftsblock.craftsnet.autoregister.loaders;

import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A loader class responsible for loading classes from a JAR file that are annotated with a specific annotation
 * (default is {@link AutoRegister}) and collecting relevant information about these classes.
 * <p>
 * This class uses an {@link ExecutorService} to parallelize the processing of classes in a JAR file.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.2.1
 * @see AutoRegisterInfo
 * @since 3.2.0-SNAPSHOT
 */
public class AutoRegisterLoader implements Closeable {

    /**
     * List of package names to skip when processing class files in the JAR.
     */
    public static final List<String> SKIP_PACKAGES = new ArrayList<>(List.of(
            "org/slf4j", "org/slf4j", "org/jetbrains", "org/intellij", "org/eclipse", "org/codehaus", "org/apache", "com/mysql",
            "com/google", "com/ctc"
    ));

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), r -> new Thread(r, "CraftsNet autoregister performer")
    );

    /**
     * Perform cleanup for the current {@link AutoRegisterLoader}
     * when it is no longer used.
     */
    @Override
    public void close() {
        executor.shutdown();
    }

    /**
     * Loads and processes all classes annotated with {@link AutoRegister} from the provided {@link JarFile} using the provided
     * class loader.
     * <p>
     * This method finds all classes in the jar that are annotated with {@link AutoRegister}, collects metadata about
     * these classes, and returns it as a list of {@link AutoRegisterInfo}.
     * </p>
     *
     * @param loader   The class loader to use (can be null for the system class loader).
     * @param bounding The list of addons that the auto register info is from. Nullable if the register info does not come from an addon.
     * @param file     The {@link JarFile} from which classes will be loaded.
     * @return A list of {@link AutoRegisterInfo} objects containing metadata about the classes.
     * @since 1.0.0
     */
    public List<AutoRegisterInfo> loadFrom(@Nullable ClassLoader loader, @Nullable Collection<Addon> bounding, @NotNull JarFile file) {
        return loadFrom(loader, bounding, file, AutoRegister.class);
    }

    /**
     * Loads and processes all classes annotated with the specified annotation from the provided {@link JarFile}.
     *
     * @param loader   The class loader to use (can be null for the system class loader).
     * @param bounding The list of addons that the auto register info is from. Nullable if the register info does not come from an addon.
     * @param file     The {@link JarFile} from which classes will be loaded.
     * @param type     The annotation type that classes should be annotated with.
     * @return A list of {@link AutoRegisterInfo} objects containing metadata about the classes.
     */
    protected List<AutoRegisterInfo> loadFrom(@Nullable ClassLoader loader, @Nullable Collection<Addon> bounding, @NotNull JarFile file,
                                              @NotNull Class<? extends Annotation> type) {
        Set<AutoRegisterInfo> infos = Collections.newSetFromMap(new ConcurrentHashMap<>());
        ClassLoader classLoader = (loader != null ? loader : ClassLoader.getSystemClassLoader());

        ConcurrentLinkedQueue<Future<?>> futures = new ConcurrentLinkedQueue<>();

        // Process each entry in the JAR file
        StreamSupport.stream(file.stream().spliterator(), true)
                .filter(this::isValidClassEntry)
                .map(JarEntry::getName)
                .map(this::convertToJvmName)
                .map(jvmName -> executor.submit(() -> processClass(jvmName, bounding, classLoader, type, infos)))
                .forEach(futures::add);

        // Wait for all futures to complete
        for (Future<?> future : futures)
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }

        futures.clear();
        return infos.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Checks whether the provided {@link JarEntry} represents a valid class file.
     *
     * @param entry The {@link JarEntry} to check.
     * @return True if the entry is a valid class file, false otherwise.
     */
    private boolean isValidClassEntry(JarEntry entry) {
        if (entry.isDirectory()) return false;

        String name = entry.getName();
        // Skip classes that belong to certain packages
        for (String skip : SKIP_PACKAGES)
            if (name.startsWith(skip)) return false;

        // Only process .class files and exclude module-info.class
        return name.endsWith(".class") && !entry.getName().endsWith("module-info.class");
    }

    /**
     * Converts a {@link JarEntry} name to a jvm class name.
     *
     * @param name The JAR entry name.
     * @return The corresponding JVM class name.
     */
    private String convertToJvmName(String name) {
        return name.substring(0, name.lastIndexOf(name.contains("$") ? "$" : ".")).replaceAll("[/\\\\]", ".");
    }

    /**
     * Processes a class by loading it, checking for the specified annotation, and collecting relevant information.
     *
     * @param jvmName     The JVM class name of the class to process.
     * @param bounding    The list of addons that the auto register info is from. Nullable if the register info does not come from an addon.
     * @param classLoader The class loader to use.
     * @param type        The annotation type to look for.
     * @param infos       A set to collect {@link AutoRegisterInfo} instances.
     */
    private void processClass(@NotNull String jvmName, @Nullable Collection<Addon> bounding, @NotNull ClassLoader classLoader,
                              @NotNull Class<? extends Annotation> type, @NotNull Set<AutoRegisterInfo> infos) {
        try {
            Class<?> clazz = Class.forName(jvmName, false, classLoader);
            if (!clazz.isAnnotationPresent(type)) return;

            Annotation annotation = clazz.getDeclaredAnnotation(type);
            List<String> parentTypes = new ArrayList<>();
            parentTypes.addAll(loadSuperclasses(clazz));
            parentTypes.addAll(loadInterfaces(clazz));

            infos.add(AutoRegisterInfo.of(jvmName, bounding, annotation, classLoader, parentTypes.stream().distinct().toList()));
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
        }
    }

    /**
     * Loads the names of all superclasses of a given class.
     *
     * @param clazz The class to load superclasses for.
     * @return A list of superclass names.
     */
    private List<String> loadSuperclasses(Class<?> clazz) {
        List<String> superclasses = new ArrayList<>();

        while (clazz != null && !clazz.equals(Object.class)) {
            clazz = clazz.getSuperclass();
            if (clazz != null)
                superclasses.add(clazz.getName());
        }

        return superclasses;
    }

    /**
     * Loads the names of all interfaces implemented by a given class and its superclasses.
     *
     * @param clazz The class to load interfaces for.
     * @return A list of interface names.
     */
    private List<String> loadInterfaces(Class<?> clazz) {
        Set<String> interfaces = new HashSet<>();
        collectInterfaces(clazz, interfaces);
        return new ArrayList<>(interfaces);
    }

    /**
     * Recursively collects all interfaces implemented by the given class and its superclasses.
     *
     * @param clazz      The class to collect interfaces for.
     * @param interfaces A set to collect interface names.
     */
    private void collectInterfaces(Class<?> clazz, Set<String> interfaces) {
        if (clazz == null || clazz.equals(Object.class)) return;

        for (Class<?> iface : clazz.getInterfaces()) {
            if (interfaces.add(iface.getName())) {
                collectInterfaces(iface, interfaces);
            }
        }

        collectInterfaces(clazz.getSuperclass(), interfaces);
    }

}
