package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Provides factory methods for argument transformation functions used by the
 * CraftsNet builder system.
 * <p>
 * Argument transformers convert string-based argument values into their
 * corresponding Java representations. They can be used to parse primitive
 * values, validate numeric ranges, instantiate classes through reflection,
 * convert comma-separated values into collections, and resolve enum constants.
 * <p>
 * Each factory method returns a {@link Function} that accepts a string and
 * produces the transformed value as an {@link Object}. These functions can
 * be registered with {@link AbstractCraftsNetBuilder#addKnownArgument(String, Function)}
 * to define how individual builder arguments are processed.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see AbstractCraftsNetBuilder
 * @see MappableState
 * @since 4.0.0
 */
public interface ArgumentTransformer {

    /**
     * An identity transformer that returns the supplied string unchanged.
     * <p>
     * This transformer is used when an argument does not require any
     * conversion or additional processing.
     */
    Function<String, Object> IDENTITY = t -> t;

    /**
     * Returns the identity transformer.
     *
     * @return A transformer that returns its input string unchanged
     */
    static Function<String, Object> identity() {
        return IDENTITY;
    }

    /**
     * Creates a transformer that converts a string into a boolean flag.
     * <p>
     * The resulting value is {@code true} if the input is blank, represents
     * {@code true} according to {@link Boolean#parseBoolean(String)}, or
     * equals {@code "1"} ignoring case. All other values result in
     * {@code false}.
     *
     * @return A transformer that converts string values into boolean flags
     */
    static Function<String, Object> booleanFlag() {
        return value -> value.isBlank() || Boolean.parseBoolean(value) || value.equalsIgnoreCase("1");
    }

    /**
     * Creates a transformer that instantiates a class specified by its fully
     * qualified class name.
     * <p>
     * The resulting instance is not restricted to a particular type beyond
     * the general {@link Object} type.
     *
     * @return A transformer that loads and instantiates the specified class
     * @see #classInstance(Class)
     */
    static Function<String, Object> classInstance() {
        return classInstance(Object.class);
    }

    /**
     * Creates a transformer that loads and instantiates a class specified by
     * its fully qualified class name.
     * <p>
     * The class is loaded using {@link Class#forName(String)} and instantiated
     * through {@link ReflectionUtils#getNewInstance(Class, Object...) }. The resulting
     * instance must be compatible with the specified guard type according to
     * {@link TypeUtils#isInstance(Class, Object)}.
     *
     * @param guard The required type that the instantiated object must satisfy
     * @return A transformer that loads, instantiates and validates the specified class
     * @throws NullPointerException     If {@code guard} is {@code null}
     * @throws RuntimeException         If the specified class cannot be found
     * @throws IllegalArgumentException If the instantiated object does not
     *                                  satisfy the specified guard type
     * @see ReflectionUtils#getNewInstance(Class, Object...)
     * @see TypeUtils#isInstance(Class, Object)
     */
    static Function<String, Object> classInstance(@NotNull Class<?> guard) {
        Objects.requireNonNull(guard, "guard");

        return value -> {
            try {
                Class<?> type = Class.forName(value);
                Object instance = ReflectionUtils.getNewInstance(type);
                if (!TypeUtils.isInstance(guard, instance)) {
                    throw new IllegalArgumentException(
                            "Class " + value + " is not a instance of " + guard.getName()
                    );
                }

                return instance;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to find class " + value, e);
            }
        };
    }

    /**
     * Creates a transformer that converts a comma-separated string into a list
     * of strings.
     * <p>
     * The input is split at every comma, and the resulting elements are stored
     * in an immutable list using {@link List#of(Object[])}.
     *
     * @return A transformer that converts comma-separated values into a list
     * @see #list(Function)
     */
    static Function<String, Object> list() {
        return value -> List.of(value.split(","));
    }

    /**
     * Creates a transformer that converts a comma-separated string into a list
     * while applying the specified transformation to each element.
     * <p>
     * The input is split at every comma. Each resulting element is passed to
     * the supplied transformer, and the transformed elements are collected
     * into an unmodifiable list.
     *
     * @param entryTransformer The transformation function applied to each list element
     * @return A transformer that converts and transforms comma-separated values
     * @throws NullPointerException If {@code entryTransformer} is {@code null}
     */
    static Function<String, Object> list(@NotNull Function<String, Object> entryTransformer) {
        Objects.requireNonNull(entryTransformer, "entryTransformer");

        return value -> Arrays.stream(value.split(","))
                .map(entryTransformer)
                .toList();
    }

    /**
     * Creates a transformer that parses a string into a byte value.
     *
     * @return A transformer that converts strings into {@link Byte} values
     * @throws NumberFormatException If the input cannot be parsed as a byte
     * @see Byte#parseByte(String)
     */
    static Function<String, Object> byteNumber() {
        return Byte::parseByte;
    }

    /**
     * Creates a transformer that parses a string into a byte value and
     * validates that it falls within the specified inclusive range.
     *
     * @param min The minimum permitted value
     * @param max The maximum permitted value
     * @return A transformer that parses and validates byte values
     * @throws NumberFormatException    If the input cannot be parsed as a byte
     * @throws IllegalArgumentException If the minimum exceeds the maximum or
     *                                  the parsed value is outside the permitted range
     * @see #validatedNumber(Number, Number, Number)
     */
    static Function<String, Object> byteNumber(byte min, byte max) {
        return value -> validatedNumber(Byte.parseByte(value), min, max);
    }

    /**
     * Creates a transformer that parses a string into a short value.
     *
     * @return A transformer that converts strings into {@link Short} values
     * @throws NumberFormatException If the input cannot be parsed as a short
     * @see Short#parseShort(String)
     */
    static Function<String, Object> shortNumber() {
        return Short::parseShort;
    }

    /**
     * Creates a transformer that parses a string into a short value and
     * validates that it falls within the specified inclusive range.
     *
     * @param min The minimum permitted value
     * @param max The maximum permitted value
     * @return A transformer that parses and validates short values
     * @throws NumberFormatException    If the input cannot be parsed as a short
     * @throws IllegalArgumentException If the minimum exceeds the maximum or
     *                                  the parsed value is outside the permitted range
     * @see #validatedNumber(Number, Number, Number)
     */
    static Function<String, Object> shortNumber(short min, short max) {
        return value -> validatedNumber(Short.parseShort(value), min, max);
    }

    /**
     * Creates a transformer that parses a string into an integer value.
     *
     * @return A transformer that converts strings into {@link Integer} values
     * @throws NumberFormatException If the input cannot be parsed as an integer
     * @see Integer#parseInt(String)
     */
    static Function<String, Object> integerNumber() {
        return Integer::parseInt;
    }

    /**
     * Creates a transformer that parses a string into an integer value and
     * validates that it falls within the specified inclusive range.
     *
     * @param min The minimum permitted value
     * @param max The maximum permitted value
     * @return A transformer that parses and validates integer values
     * @throws NumberFormatException    If the input cannot be parsed as an integer
     * @throws IllegalArgumentException If the minimum exceeds the maximum or
     *                                  the parsed value is outside the permitted range
     * @see #validatedNumber(Number, Number, Number)
     */
    static Function<String, Object> integerNumber(int min, int max) {
        return value -> validatedNumber(Integer.parseInt(value), min, max);
    }

    /**
     * Creates a transformer that parses a string into a long value.
     *
     * @return A transformer that converts strings into {@link Long} values
     * @throws NumberFormatException If the input cannot be parsed as a long
     * @see Long#parseLong(String)
     */
    static Function<String, Object> longNumber() {
        return Long::parseLong;
    }

    /**
     * Creates a transformer that parses a string into a long value and
     * validates that it falls within the specified inclusive range.
     *
     * @param min The minimum permitted value
     * @param max The maximum permitted value
     * @return A transformer that parses and validates long values
     * @throws NumberFormatException    If the input cannot be parsed as a long
     * @throws IllegalArgumentException If the minimum exceeds the maximum or
     *                                  the parsed value is outside the permitted range
     * @see #validatedNumber(Number, Number, Number)
     */
    static Function<String, Object> longNumber(long min, long max) {
        return value -> validatedNumber(Long.parseLong(value), min, max);
    }

    /**
     * Creates a transformer that parses a string into a float value.
     *
     * @return A transformer that converts strings into {@link Float} values
     * @throws NumberFormatException If the input cannot be parsed as a float
     * @see Float#parseFloat(String)
     */
    static Function<String, Object> floatNumber() {
        return Float::parseFloat;
    }

    /**
     * Creates a transformer that parses a string into a float value and
     * validates that it falls within the specified inclusive range.
     *
     * @param min The minimum permitted value
     * @param max The maximum permitted value
     * @return A transformer that parses and validates float values
     * @throws NumberFormatException    If the input cannot be parsed as a float
     * @throws IllegalArgumentException If the minimum exceeds the maximum or
     *                                  the parsed value is outside the permitted range
     * @see #validatedNumber(Number, Number, Number)
     */
    static Function<String, Object> floatNumber(float min, float max) {
        return value -> validatedNumber(Float.parseFloat(value), min, max);
    }

    /**
     * Creates a transformer that parses a string into a double-precision
     * floating-point value.
     *
     * @return A transformer that converts strings into {@link Double} values
     * @throws NumberFormatException If the input cannot be parsed as a double
     * @see Double#parseDouble(String)
     */
    static Function<String, Object> decimalNumber() {
        return Double::parseDouble;
    }

    /**
     * Creates a transformer that parses a string into a double-precision
     * floating-point value and validates that it falls within the specified
     * inclusive range.
     *
     * @param min The minimum permitted value
     * @param max The maximum permitted value
     * @return A transformer that parses and validates double values
     * @throws NumberFormatException    If the input cannot be parsed as a double
     * @throws IllegalArgumentException If the minimum exceeds the maximum or
     *                                  the parsed value is outside the permitted range
     * @see #validatedNumber(Number, Number, Number)
     */
    static Function<String, Object> decimalNumber(double min, double max) {
        return value -> validatedNumber(Double.parseDouble(value), min, max);
    }

    /**
     * Validates that a numeric value falls within the specified inclusive range.
     * <p>
     * All three numbers are converted to {@code double} for comparison.
     * The minimum must not exceed the maximum, and the supplied value must
     * be finite and greater than or equal to the minimum and less than or
     * equal to the maximum.
     *
     * @param <N>   The numeric type
     * @param value The value to validate
     * @param min   The inclusive lower bound
     * @param max   The inclusive upper bound
     * @return The original value if it satisfies the specified constraints
     * @throws NullPointerException     If {@code value}, {@code min} or {@code max} is {@code null}
     * @throws IllegalArgumentException If the minimum exceeds the maximum,
     *                                  or if the value is non-finite or outside the permitted range
     */
    static <N extends Number> N validatedNumber(@NotNull N value, @NotNull N min, @NotNull N max) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");

        double parsed = value.doubleValue();
        double minimum = min.doubleValue();
        double maximum = max.doubleValue();

        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum must not exceed maximum");
        }

        if (!Double.isFinite(parsed) || parsed < minimum || parsed > maximum) {
            throw new IllegalArgumentException(
                    "Value must be between " + min + " and " + max
            );
        }

        return value;
    }

    /**
     * Creates a transformer that resolves a string to an enum constant.
     * <p>
     * The input is converted to uppercase before being passed to
     * {@link Enum#valueOf(Class, String)}. If the resolved constant implements
     * {@link MappableState}, its mapped target state is returned instead
     * of the original constant.
     *
     * @param <E>  The enum type
     * @param type The enum class used to resolve the input value
     * @return A transformer that resolves enum constants and applies optional
     * {@link MappableState} mappings
     * @throws NullPointerException     If {@code type} is {@code null}
     * @throws IllegalArgumentException If no matching enum constant exists
     */
    static <E extends Enum<E>> Function<String, Object> enumType(@NotNull Class<E> type) {
        Objects.requireNonNull(type, "type");

        return v -> {
            E value = Enum.valueOf(type, v.toUpperCase());
            if (value instanceof MappableState<?> mappable) {
                return mappable.getMapTo();
            }

            return value;
        };
    }

    /**
     * Creates a transformer that resolves a string to an enum constant and
     * applies a custom mapping function to the resolved value.
     * <p>
     * The input is converted to uppercase before being passed to
     * {@link Enum#valueOf(Class, String)}. The resulting enum constant is then
     * passed to the supplied mapper, whose return value becomes the
     * transformation result.
     *
     * @param <E>    The enum type
     * @param type   The enum class used to resolve the input value
     * @param mapper The function used to map the resolved enum constant
     * @return A transformer that resolves and maps enum constants
     * @throws NullPointerException     If {@code type} or {@code mapper} is {@code null}
     * @throws IllegalArgumentException If no matching enum constant exists
     */
    static <E extends Enum<E>> Function<String, Object> enumType(@NotNull Class<E> type, @NotNull Function<E, E> mapper) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(mapper, "mapper");

        return value -> mapper.apply(Enum.valueOf(type, value.toUpperCase()));
    }

    /**
     * Creates a transformer that rejects every attempted argument assignment
     * with a default error message.
     * <p>
     * This transformer is useful for arguments that must not be configured
     * through string-based input, such as command-line interfaces.
     *
     * @return A transformer that always throws an {@link IllegalArgumentException}
     * @see #notAllowed(String)
     */
    static Function<String, Object> notAllowed() {
        return notAllowed(null);
    }

    /**
     * Creates a transformer that rejects every attempted argument assignment
     * with the specified error message.
     * <p>
     * The supplied input value is not processed. Invoking the returned
     * transformer always results in an {@link IllegalArgumentException}.
     * If the message is {@code null}, a default message is used.
     *
     * @param message The error message to include in the exception, or
     *                {@code null} to use the default message
     * @return A transformer that always throws an {@link IllegalArgumentException}
     */
    static Function<String, Object> notAllowed(String message) {
        return value -> {
            throw new IllegalArgumentException(message == null ? "Not allowed to set this via the cli" : message);
        };
    }

}
