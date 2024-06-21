package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This utility class provides helper methods for thread-related operations.
 * It includes functionality to retrieve threads by their names.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
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

    /**
     * Splits the given byte array into parts, using the specified delimiter byte array. The delimiter
     * is not included in the resulting parts. If the delimiter is not found, the entire byte array is
     * returned as a single part.
     *
     * @param data      the byte array to be split. Must not be null.
     * @param delimiter the byte array that serves as the delimiter. Must not be null.
     * @return a list of byte arrays, each representing a part of the original array split by the delimiter.
     * @throws NullPointerException if {@code data} or {@code delimiter} is null.
     */
    public static List<byte[]> splitByteArray(byte[] data, byte[] delimiter) {
        List<byte[]> parts = new ArrayList<>();
        int start = 0;
        int delimiterLength = delimiter.length;

        while (start <= data.length) {
            int idx = indexOf(data, delimiter, start);

            if (idx == -1) {
                byte[] part = new byte[data.length - start];
                System.arraycopy(data, start, part, 0, data.length - start);
                parts.add(part);
                break;
            }

            byte[] part = new byte[idx - start];
            System.arraycopy(data, start, part, 0, idx - start);
            parts.add(part);

            start = idx + delimiterLength;
        }

        return parts;
    }

    /**
     * Finds the first occurrence of the target byte array within the source byte array, starting from the specified index.
     * Uses a simple brute-force search algorithm.
     *
     * @param data   the source byte array. Must not be null.
     * @param target the target byte array to find. Must not be null.
     * @param start  the starting index for the search. Must be non-negative and less than or equal to {@code data.length}.
     * @return the index of the first occurrence of the target byte array within the source byte array,
     *         or -1 if the target is not found.
     * @throws NullPointerException      if {@code data} or {@code target} is null.
     * @throws IndexOutOfBoundsException if {@code start} is negative or greater than {@code data.length}.
     */
    private static int indexOf(byte[] data, byte[] target, int start) {
        outer:
        for (int i = start; i <= data.length - target.length; i++) {
            for (int j = 0; j < target.length; j++)
                if (data[i + j] != target[j])
                    continue outer;
            return i;
        }
        return -1;
    }

}
