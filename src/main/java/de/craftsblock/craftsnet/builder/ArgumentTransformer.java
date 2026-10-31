package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;

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
        return classInstance(Objects.class);
    }

    static ArgumentTransformer classInstance(Class<?> guard) {
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

    static ArgumentTransformer list(ArgumentTransformer entryTransformer) {
        return value -> Arrays.stream(value.split(","))
                .map(entryTransformer)
                .toList();
    }

    static ArgumentTransformer number(long min, long max) {
        return value -> validatedNumber(Long.parseLong(value), min, max);
    }

    static long validatedNumber(long value, long min, long max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "Value must be between " + min + " and " + max
            );
        }

        return value;
    }

    static <E extends Enum<E>> ArgumentTransformer enumType(Class<E> type) {
        return v -> {
            E value = Enum.valueOf(type, v.toUpperCase());
            if (value instanceof MappableState<?> mappable) {
                return mappable.getMapTo();
            }

            return value;
        };
    }

    static <E extends Enum<E>> ArgumentTransformer enumType(Class<E> type, Function<E, E> mapper) {
        return value -> mapper.apply(Enum.valueOf(type, value.toUpperCase()));
    }

    static ArgumentTransformer notAllowed(String message) {
        return value -> {
            throw new IllegalArgumentException(message == null ? "Not allowed to set this via the cli" : message);
        };
    }

}
