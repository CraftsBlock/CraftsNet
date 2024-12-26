package de.craftsblock.craftsnet.utils.versions;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing comparison operations for version strings. The {@link Comparison} enum provides functionality to
 * compare version strings (in the format of "x.y.z") using common comparison operators.
 * <p>
 * It supports comparing version strings by splitting them into their individual components (e.g., major, minor, patch),
 * and applies the appropriate comparison operation.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0
 * @since 3.1.0-SNAPSHOT
 */
enum Comparison {

    LESS("<"),
    GREATER(">"),
    LESS_OR_EQUAL("<="),
    GREATER_OR_EQUAL(">="),
    EQUAL("=");

    private static final Map<String, Comparison> SYMBOLS = new HashMap<>();

    static {
        for (Comparison comparison : Comparison.values())
            SYMBOLS.put(comparison.symbol, comparison);
    }

    private final String symbol;

    /**
     * Constructs a {@code Comparison} with the provided symbol.
     *
     * @param symbol the string symbol representing the comparison operation
     */
    Comparison(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Compares two version strings using the current comparison operator.
     * The version strings are sanitized, split by dots ('.'), and each part is compared lexicographically as an integer.
     * The comparison stops as soon as a difference is found, and the appropriate comparison operation is applied.
     *
     * @param current  the current version to compare
     * @param expected the expected version to compare against
     * @return true if the comparison is suitable based on the operator; false otherwise
     */
    public boolean suitable(String current, String expected) {
        String[] cs = sanitize(current).split("\\.");
        String[] es = sanitize(expected).split("\\.");

        int length = Math.max(cs.length, es.length);

        for (int i = 0; i < length; i++) {
            int c = parsePart(cs, i);
            int e = parsePart(es, i);

            if (e == Integer.MIN_VALUE) continue;

            if (c < e) return apply(-1);
            else if (c > e) return apply(1);
        }

        return apply(0);
    }

    /**
     * Parses a part of the version string into an integer.
     * If the part at the specified index is not present or cannot be parsed, it returns 0.
     *
     * @param parts the array of version parts
     * @param index the index of the part to parse
     * @return the parsed integer value of the version part, or 0 if not valid
     */
    private int parsePart(String[] parts, int index) {
        if (index >= parts.length) return 0;
        try {
            String part = parts[index];
            if (part.equals("*")) return Integer.MIN_VALUE;
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Sanitizes the version string by trimming any whitespace and ensuring that null or empty strings are treated as "0".
     *
     * @param version the version string to sanitize
     * @return the sanitized version string
     */
    private String sanitize(String version) {
        return version == null || version.isEmpty() ? "0" : version.trim();
    }

    /**
     * Applies the current comparison operation to the result of the version comparison.
     *
     * @param result the result of the comparison: -1 (less than), 0 (equal), or 1 (greater than)
     * @return true if the comparison satisfies the operation, false otherwise
     */
    public boolean apply(int result) {
        return switch (this) {
            case LESS -> result < 0;
            case GREATER -> result > 0;
            case LESS_OR_EQUAL -> result <= 0;
            case GREATER_OR_EQUAL -> result >= 0;
            case EQUAL -> result == 0;
        };
    }

    /**
     * Retrieves the {@link Comparison} enum value corresponding to the provided symbol.
     * <p>
     * If the symbol is not found, the {@link #EQUAL} comparison is returned by default.
     * </p>
     *
     * @param symbol the symbol representing the comparison operation
     * @return the corresponding {@link Comparison} enum value, or {@link #EQUAL} if the symbol is not found
     */
    public static Comparison from(String symbol) {
        return SYMBOLS.getOrDefault(symbol, Comparison.EQUAL);
    }

}
