package de.craftsblock.craftsnet.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This utility class provides helper methods for generating passphrases.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.0
 * @since 3.3.5-SNAPSHOT
 */
public class PassphraseUtils {

    /**
     * The secure random instance used for generating passphrases.
     */
    private static final SecureRandom SECURE_RANDOM;

    /**
     * The base chars used to created passphrases.
     */
    public static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * The special chars used to created passphrases when special chars are required.
     */
    public static final String SPECIAL_CHARS = "!$&_-#";

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Private constructor to prevent direct instantiation
     */
    private PassphraseUtils() {
    }

    /**
     * Generates a secure random passphrase with a default length between 12 and 16 characters.
     * The passphrase can include digits, uppercase and lowercase letters, and special characters.
     *
     * @return a secure random passphrase as a {@link String}.
     */
    public static String generateSecure() {
        return generateSecure(true);
    }

    /**
     * Generates a secure random passphrase with a default length between 12 and 16 characters.
     * The passphrase can include digits, uppercase and lowercase letters, and optionally special
     * characters.
     *
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@link String}.
     */
    public static String generateSecure(boolean specialChars) {
        return generateSecure(12, 16, specialChars);
    }

    /**
     * Generates a secure random passphrase with a specified length and the option to include
     * special characters. The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and special characters.
     *
     * @param length the length of the generated passphrase.
     * @return a secure random passphrase as a {@link String}.
     */
    public static String generateSecure(int length) {
        return generateSecure(length, true);
    }

    /**
     * Generates a secure random passphrase with a specified length and the option to include
     * special characters. The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and optionally special characters.
     *
     * @param length       the length of the generated passphrase.
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@link String}.
     */
    public static String generateSecure(int length, boolean specialChars) {
        return generateSecure(length, length, specialChars);
    }

    /**
     * Generates a secure random passphrase with a specified length range and
     * the option to include special characters.
     * The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and optionally special characters.
     *
     * @param min          the minimum length of the generated passphrase (inclusive).
     * @param max          the maximum length of the generated passphrase (exclusive).
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@link String}.
     */
    public static String generateSecure(int min, int max, boolean specialChars) {
        Stream<Character> stream;
        if (specialChars)
            stream = IntStream.concat(CHARS.chars(), SPECIAL_CHARS.chars()).mapToObj(c -> (char) c);
        else stream = CHARS.chars().mapToObj(c -> (char) c);

        List<Character> chars = stream.toList();

        return SECURE_RANDOM.ints(secureRandomLength(min, max), 0, chars.size())
                .mapToObj(chars::get)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * Generates a secure random passphrase length within a specified range.
     * The length is chosen randomly from the given range using a secure random number generator.
     *
     * @param min the minimum length (inclusive).
     * @param max the maximum length (exclusive).
     * @return a randomly generated length within the specified range.
     */
    public static int secureRandomLength(int min, int max) {
        if (min == max) return min;
        return SECURE_RANDOM.nextInt(min, max);
    }

}
