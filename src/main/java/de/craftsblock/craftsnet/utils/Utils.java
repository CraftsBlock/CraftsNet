package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * This utility class provides helper methods for thread-related operations.
 * It includes functionality to retrieve threads by their names.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-2.1.1
 */
public class Utils {

    /**
     * A regular expression pattern used to extract group names from a regular expression pattern string.
     * This pattern matches named capturing groups defined in regular expression patterns.
     */
    public static final Pattern patternGroupNameExtractPattern = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

    /**
     * Retrieves a thread by its name from the currently running threads.
     *
     * <p>This method searches through all currently running threads and returns
     * the thread with the specified name. If no thread with the given name is
     * found, it returns null.</p>
     *
     * @param name The name of the thread to retrieve.
     * @return The thread with the specified name, or null if not found.
     * @throws SecurityException If a security manager exists and its
     *                           {@link SecurityManager#checkPermission(Permission)} method
     *                           denies access to the current thread.
     */
    @Nullable
    public static Thread getThreadByName(String name) {
        Thread[] threads = Thread.getAllStackTraces().keySet().toArray(new Thread[0]);
        for (Thread t : threads) if (t.getName().equals(name)) return t;
        return null;
    }

}
