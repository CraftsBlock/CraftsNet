package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * This utility class provides helper methods for generating passphrases.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 2.1.0
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
     * @return a secure random passphrase as a {@code byte[]}.
     */
    public static byte[] generateSecure() {
        return generateSecure(true);
    }

    /**
     * Generates a secure random passphrase with a default length between 12 and 16 characters.
     * The passphrase can include digits, uppercase and lowercase letters, and optionally special
     * characters.
     *
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@code byte[]}.
     */
    public static byte[] generateSecure(boolean specialChars) {
        return generateSecure(12, 16, specialChars);
    }

    /**
     * Generates a secure random passphrase with a specified length and the option to include
     * special characters. The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and special characters.
     *
     * @param length the length of the generated passphrase.
     * @return a secure random passphrase as a {@code byte[]}.
     */
    public static byte[] generateSecure(@Range(from = 1, to = Integer.MAX_VALUE) int length) {
        return generateSecure(length, true);
    }

    /**
     * Generates a secure random passphrase with a specified length and the option to include
     * special characters. The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and optionally special characters.
     *
     * @param length       the length of the generated passphrase.
     * @param specialChars whether to include special characters in the passphrase.
     * @return a secure random passphrase as a {@code byte[]}.
     */
    public static byte[] generateSecure(@Range(from = 1, to = Integer.MAX_VALUE) int length, boolean specialChars) {
        return generateSecure(length, length, specialChars);
    }

    /**
     * Generates a secure random passphrase with a specified length range and
     * the option to include special characters.
     * The passphrase is generated from a character pool consisting of digits,
     * lowercase and uppercase letters, and optionally special characters.
     *
     * @param min          The minimum length of the generated passphrase (inclusive).
     * @param max          The maximum length of the generated passphrase (exclusive).
     * @param specialChars Whether to include special characters in the passphrase.
     * @return A secure random passphrase as a {@code byte[]}.
     */
    public static byte[] generateSecure(@Range(from = 1, to = Integer.MAX_VALUE) int min, @Range(from = 1, to = Integer.MAX_VALUE) int max,
                                        boolean specialChars) {
        return generateSecure(min, max, CHARS + (specialChars ? SPECIAL_CHARS : ""));
    }

    /**
     * Generates a secure random passphrase with a specified length range.
     * The passphrase is generated from the specified character pool.
     *
     * @param min   The minimum length of the generated passphrase (inclusive).
     * @param max   The maximum length of the generated passphrase (exclusive).
     * @param chars The character pool from which the passphrase will be generated.
     *              Must include more than 15 characters.
     * @return A secure random passphrase as a {@code byte[]}.
     * @since 3.4.3-SNAPSHOT
     */
    public static byte[] generateSecure(@Range(from = 1, to = Integer.MAX_VALUE) int min, @Range(from = 1, to = Integer.MAX_VALUE) int max,
                                        @NotNull String chars) {
        if (chars.length() < 15)
            throw new IllegalArgumentException("The character pool for generating passphrases must not be short than 15!");

        List<Character> characters = chars.chars().mapToObj(c -> (char) c).toList();

        int[] length = new int[]{secureRandomLength(min, max)};
        char[] passphraseChars = new char[length[0]];

        for (int i = 0; i < length[0]; i++) {
            int index = SECURE_RANDOM.nextInt(characters.size());
            passphraseChars[i] = characters.get(index);
        }

        Arrays.fill(length, 0);

        byte[] passphrase = SecureEncodingUtils.encode(passphraseChars, StandardCharsets.UTF_8);
        erase(passphraseChars);
        return passphrase;
    }

    /**
     * Overwrites the contents of the given byte array with zeros.
     * This is useful for securely erasing sensitive data such as passwords
     * from memory to reduce the risk of leaking secrets.
     *
     * @param passphrase The byte array to be cleared; must not be null
     * @since 3.4.1-SNAPSHOT
     */
    public static void erase(byte @NotNull [] passphrase) {
        Arrays.fill(passphrase, (byte) 0);
    }

    /**
     * Overwrites the contents of the given char array with null characters ('\0').
     * This is useful for securely erasing sensitive data such as passwords
     * from memory to reduce the risk of leaking secrets.
     *
     * @param passphrase The char array to be cleared; must not be null
     * @since 3.4.1-SNAPSHOT
     */
    public static void erase(char @NotNull [] passphrase) {
        Arrays.fill(passphrase, '\0');
    }

    /**
     * Converts the given UTF-8 encoded byte array into a {@link String}.
     *
     * <p><strong>WARNING:</strong> This method converts sensitive byte arrays
     * (e.g., passwords) into a {@link String}, which is immutable in Java and
     * cannot be explicitly cleared from memory. This may lead to sensitive data
     * lingering in memory longer than necessary and increase the risk of
     * information leakage.</p>
     *
     * <p>Use this method only if you fully understand the security implications.</p>
     *
     * @param passphrase The byte array containing UTF-8 encoded characters; must not be null
     * @return A String representing the decoded characters
     * @throws NullPointerException If passphrase is null
     */
    public static String stringify(byte @NotNull [] passphrase) {
        return new String(passphrase, StandardCharsets.UTF_8);
    }

    /**
     * Generates a secure random passphrase length within a specified range.
     * The length is chosen randomly from the given range using a secure random number generator.
     *
     * @param min The minimum length (inclusive).
     * @param max The maximum length (exclusive).
     * @return A randomly generated length within the specified range.
     */
    public static int secureRandomLength(@Range(from = 1, to = Integer.MAX_VALUE) int min, @Range(from = 1, to = Integer.MAX_VALUE) int max) {
        if (min > max)
            throw new IllegalArgumentException("The min (%s) must not be grater than max (%s)!".formatted(
                    min, max
            ));

        if (min == max) return min;
        return SECURE_RANDOM.nextInt(min, max);
    }

}
