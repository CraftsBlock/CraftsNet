package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.Nullable;

import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class provides helper methods for thread-related operations.
 * It includes functionality to retrieve threads by their names.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.6
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

    /**
     * Generates a secure random passphrase with a default length between 12 and 16 characters.
     * The passphrase can include digits, uppercase and lowercase letters, and special characters.
     *
     * @return a secure random passphrase as a {@link String}.
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static String secureRandomPassphrase() throws NoSuchAlgorithmException {
        return secureRandomPassphrase(true);
    }

    /**
     * Generates a secure random passphrase with a default length between 12 and 16 characters.
     * The passphrase can include digits, uppercase and lowercase letters, and optionally special
     * characters.
     *
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@link String}.
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static String secureRandomPassphrase(boolean specialChars) throws NoSuchAlgorithmException {
        return secureRandomPassphrase(12, 16, specialChars);
    }

    /**
     * Generates a secure random passphrase with a specified length and the option to include
     * special characters. The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and special characters.
     *
     * @param length the length of the generated passphrase.
     * @return a secure random passphrase as a {@link String}.
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static String secureRandomPassphrase(int length) throws NoSuchAlgorithmException {
        return secureRandomPassphrase(length, true);
    }

    /**
     * Generates a secure random passphrase with a specified length and the option to include
     * special characters. The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and optionally special characters.
     *
     * @param length       the length of the generated passphrase.
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@link String}.
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static String secureRandomPassphrase(int length, boolean specialChars) throws NoSuchAlgorithmException {
        return secureRandomPassphrase(length, length, specialChars);
    }

    /**
     * Generates a secure random passphrase with a specified length range and
     * the option to include special characters.
     * The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and optionally special characters.
     *
     * @param origin       the minimum length of the generated passphrase (inclusive).
     * @param bound        the maximum length of the generated passphrase (exclusive).
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@link String}.
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static String secureRandomPassphrase(int origin, int bound, boolean specialChars) throws NoSuchAlgorithmException {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" + (specialChars ? "!$&_-#" : "");
        return SecureRandom.getInstanceStrong()
                .ints(secureRandomPassphraseLength(origin, bound), 0, chars.length())
                .mapToObj(chars::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * Generates a secure random passphrase length within a specified range.
     * The length is chosen randomly from the given range using a secure random number generator.
     *
     * @param origin the minimum length (inclusive).
     * @param bound  the maximum length (exclusive).
     * @return a randomly generated length within the specified range.
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static int secureRandomPassphraseLength(int origin, int bound) throws NoSuchAlgorithmException {
        if (origin == bound) return origin;
        return SecureRandom.getInstanceStrong().nextInt(origin, bound);
    }

}
