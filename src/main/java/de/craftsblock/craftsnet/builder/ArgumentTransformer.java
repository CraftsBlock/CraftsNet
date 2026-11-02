package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public interface ArgumentTransformer {

    Function<String, Object> IDENTITY = t -> t;

    static Function<String, Object> identity() {
        return IDENTITY;
    }

    static Function<String, Object> booleanFlag() {
        return value -> value.isBlank() || Boolean.parseBoolean(value) || value.equalsIgnoreCase("1");
    }

    static Function<String, Object> classInstance() {
        return classInstance(Object.class);
    }

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

    static Function<String, Object> list() {
        return value -> List.of(value.split(","));
    }

    static Function<String, Object> list(@NotNull Function<String, Object> entryTransformer) {
        Objects.requireNonNull(entryTransformer, "entryTransformer");

        return value -> Arrays.stream(value.split(","))
                .map(entryTransformer)
                .toList();
    }

    static Function<String, Object> byteNumber() {
        return Byte::parseByte;
    }

    static Function<String, Object> byteNumber(byte min, byte max) {
        return value -> validatedNumber(Byte.parseByte(value), min, max);
    }

    static Function<String, Object> shortNumber() {
        return Short::parseShort;
    }

    static Function<String, Object> shortNumber(short min, short max) {
        return value -> validatedNumber(Short.parseShort(value), min, max);
    }

    static Function<String, Object> integerNumber() {
        return Integer::parseInt;
    }

    static Function<String, Object> integerNumber(int min, int max) {
        return value -> validatedNumber(Integer.parseInt(value), min, max);
    }

    static Function<String, Object> longNumber() {
        return Long::parseLong;
    }

    static Function<String, Object> longNumber(long min, long max) {
        return value -> validatedNumber(Long.parseLong(value), min, max);
    }

    static Function<String, Object> floatNumber() {
        return Float::parseFloat;
    }

    static Function<String, Object> floatNumber(float min, float max) {
        return value -> validatedNumber(Float.parseFloat(value), min, max);
    }

    static Function<String, Object> decimalNumber() {
        return Double::parseDouble;
    }

    static Function<String, Object> decimalNumber(double min, double max) {
        return value -> validatedNumber(Double.parseDouble(value), min, max);
    }

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

    static <E extends Enum<E>> Function<String, Object> enumType(@NotNull Class<E> type, @NotNull Function<E, E> mapper) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(mapper, "mapper");

        return value -> mapper.apply(Enum.valueOf(type, value.toUpperCase()));
    }

    static Function<String, Object> notAllowed() {
        return notAllowed(null);
    }

    static Function<String, Object> notAllowed(String message) {
        return value -> {
            throw new IllegalArgumentException(message == null ? "Not allowed to set this via the cli" : message);
        };
    }

}
