package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.Nullable;

import java.security.Permission;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class provides helper methods for general operations.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.0
 * @since 2.1.1-SNAPSHOT
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

    /**
     * Extracts the group names of a {@link Pattern}.
     *
     * @param pattern The pattern, from which the group names should be extracted.
     * @return A {@link List<String>} which contains the group names in the right order.
     */
    public static List<String> getGroupNames(Pattern pattern) {
        return getGroupNames(pattern.pattern());
    }

    /**
     * Extracts the group names of a {@link Pattern}.
     *
     * @param regex The pattern, from which the group names should be extracted.
     * @return A {@link List<String>} which contains the group names in the right order.
     */
    public static List<String> getGroupNames(String regex) {
        Set<String> groupNames = new LinkedHashSet<>();
        Matcher matcher = patternGroupNameExtractPattern.matcher(regex);
        while (matcher.find()) groupNames.add(matcher.group(1));
        return new ArrayList<>(groupNames);
    }

}
