package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public interface ArgumentTransformer extends Function<String, Object> {

    ArgumentTransformer IDENTITY = t -> t;

    static ArgumentTransformer identity() {
        return IDENTITY;
    }

    static ArgumentTransformer booleanFlag() {
        return value -> value.isBlank() || Boolean.parseBoolean(value) || value.equalsIgnoreCase("1");
    }

    static ArgumentTransformer classInstance() {
        return classInstance(Object.class);
    }

    static ArgumentTransformer classInstance(@NotNull Class<?> guard) {
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

    static ArgumentTransformer list() {
        return value -> List.of(value.split(","));
    }

    static ArgumentTransformer list(@NotNull ArgumentTransformer entryTransformer) {
        Objects.requireNonNull(entryTransformer, "entryTransformer");

        return value -> Arrays.stream(value.split(","))
                .map(entryTransformer)
                .toList();
    }

    static ArgumentTransformer byteNumber() {
        return Byte::parseByte;
    }

    static ArgumentTransformer byteNumber(byte min, byte max) {
        return value -> validatedNumber(Byte.parseByte(value), min, max);
    }

    static ArgumentTransformer shortNumber() {
        return Short::parseShort;
    }

    static ArgumentTransformer shortNumber(short min, short max) {
        return value -> validatedNumber(Short.parseShort(value), min, max);
    }

    static ArgumentTransformer integerNumber() {
        return Integer::parseInt;
    }

    static ArgumentTransformer integerNumber(int min, int max) {
        return value -> validatedNumber(Integer.parseInt(value), min, max);
    }

    static ArgumentTransformer longNumber() {
        return Long::parseLong;
    }

    static ArgumentTransformer longNumber(long min, long max) {
        return value -> validatedNumber(Long.parseLong(value), min, max);
    }

    static ArgumentTransformer floatNumber() {
        return Float::parseFloat;
    }

    static ArgumentTransformer floatNumber(float min, float max) {
        return value -> validatedNumber(Float.parseFloat(value), min, max);
    }

    static ArgumentTransformer decimalNumber() {
        return Double::parseDouble;
    }

    static ArgumentTransformer decimalNumber(double min, double max) {
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

    static <E extends Enum<E>> ArgumentTransformer enumType(@NotNull Class<E> type) {
        Objects.requireNonNull(type, "type");

        return v -> {
            E value = Enum.valueOf(type, v.toUpperCase());
            if (value instanceof MappableState<?> mappable) {
                return mappable.getMapTo();
            }

            return value;
        };
    }

    static <E extends Enum<E>> ArgumentTransformer enumType(@NotNull Class<E> type, @NotNull Function<E, E> mapper) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(mapper, "mapper");

        return value -> mapper.apply(Enum.valueOf(type, value.toUpperCase()));
    }

    static ArgumentTransformer notAllowed() {
        return notAllowed(null);
    }

    static ArgumentTransformer notAllowed(String message) {
        return value -> {
            throw new IllegalArgumentException(message == null ? "Not allowed to set this via the cli" : message);
        };
    }

}
