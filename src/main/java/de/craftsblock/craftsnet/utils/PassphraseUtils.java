package de.craftsblock.craftsnet.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * This utility class provides helper methods for generating passphrases.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.5-SNAPSHOT
 */
public class PassphraseUtils {

    /**
     * Generates a secure random passphrase with a default length between 12 and 16 characters.
     * The passphrase can include digits, uppercase and lowercase letters, and special characters.
     *
     * @return a secure random passphrase as a {@link String}.
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static String generateSecure() throws NoSuchAlgorithmException {
        return generateSecure(true);
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
    public static String generateSecure(boolean specialChars) throws NoSuchAlgorithmException {
        return generateSecure(12, 16, specialChars);
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
    public static String generateSecure(int length) throws NoSuchAlgorithmException {
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
     * @throws NoSuchAlgorithmException if no strong secure random algorithm is available.
     */
    public static String generateSecure(int length, boolean specialChars) throws NoSuchAlgorithmException {
        return generateSecure(length, length, specialChars);
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
    public static String generateSecure(int origin, int bound, boolean specialChars) throws NoSuchAlgorithmException {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" + (specialChars ? "!$&_-#" : "");
        return SecureRandom.getInstanceStrong()
                .ints(secureRandomLength(origin, bound), 0, chars.length())
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
    public static int secureRandomLength(int origin, int bound) throws NoSuchAlgorithmException {
        if (origin == bound) return origin;
        return SecureRandom.getInstanceStrong().nextInt(origin, bound);
    }

}
