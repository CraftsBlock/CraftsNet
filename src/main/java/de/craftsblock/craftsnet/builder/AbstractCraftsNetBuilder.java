package de.craftsblock.craftsnet.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractCraftsNetBuilder<T extends AbstractCraftsNetBuilder<T>> {

    private T owner;
    private boolean frozen = false;

    private final Map<String, Object> args = new HashMap<>();
    private final Map<String, Function<String, Object>> argTransformers = new HashMap<>();

    public AbstractCraftsNetBuilder() {
        this(Map.of());
    }

    public AbstractCraftsNetBuilder(@NotNull Map<String, Function<String, Object>> argTransformers) {
        Objects.requireNonNull(argTransformers, "argTransformers").forEach(this::setArgTransformer);
    }

    protected void setArgTransformer(@NotNull String arg, @NotNull Function<String, Object> transformer) {
        Objects.requireNonNull(arg, "arg");
        Objects.requireNonNull(transformer, "transformer");

        ensureNotFrozen();
        this.argTransformers.put(arg.toLowerCase(), transformer);
    }

    protected final void acquire(@NotNull T owner) {
        ensureNotFrozen();

        if (this.owner != null) {
            throw new IllegalStateException("The builder is already known by a root");
        }

        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public @NotNull T set(@NotNull String arg) {
        return this.set(arg, "");
    }

    public @NotNull T set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");
        Objects.requireNonNull(value, "value");

        ensureNotFrozen();

        Object transformed;
        if (value instanceof String string) {
            transformed = this.argTransformers.getOrDefault(arg.toLowerCase(), s -> s).apply(string);
        } else {
            transformed = value;
        }

        this.args.put(arg.toLowerCase(), transformed);
        return owner;
    }

    public @Nullable Object get(@NotNull String arg) {
        Objects.requireNonNull(arg, "arg");

        return this.args.get(arg);
    }

    @SuppressWarnings("unchecked")
    public <E> @Nullable E getTyped(@NotNull String arg, @NotNull Class<E> type) {
        Objects.requireNonNull(arg, "arg");
        Objects.requireNonNull(type, "type");

        Object value = this.get(arg);
        if (value == null) {
            return null;
        }

        return (E) value;
    }

    public @Nullable String getString(@NotNull String arg) {
        return this.getTyped(arg, String.class);
    }

    public @Nullable Boolean getBoolean(@NotNull String arg) {
        return this.getTyped(arg, Boolean.class);
    }

    public @Nullable Byte getByte(@NotNull String arg) {
        return this.getTyped(arg, Byte.class);
    }

    public @Nullable Short getShort(@NotNull String arg) {
        return this.getTyped(arg, Short.class);
    }

    public @Nullable Integer getInt(@NotNull String arg) {
        return this.getTyped(arg, Integer.class);
    }

    public @Nullable Long getLong(@NotNull String arg) {
        return this.getTyped(arg, Long.class);
    }

    public @Nullable Float getFloat(@NotNull String arg) {
        return this.getTyped(arg, Float.class);
    }

    public @Nullable Double getDouble(@NotNull String arg) {
        return this.getTyped(arg, Double.class);
    }

    public <E extends Enum<E>> @Nullable E getEnum(@NotNull String arg, @NotNull Class<E> type) {
        return this.getTyped(arg, type);
    }

    public @Nullable Collection<?> getCollection(@NotNull String arg) {
        return this.getTyped(arg, Collection.class);
    }

    @SuppressWarnings("unchecked")
    public <E> @Nullable Collection<E> getCollection(@NotNull String arg, @NotNull Class<E> subType) {
        Objects.requireNonNull(subType, "subType");

        return (Collection<E>) getCollection(arg);
    }

    public @Nullable Map<?, ?> getMap(@NotNull String arg) {
        return this.getTyped(arg, Map.class);
    }

    @SuppressWarnings("unchecked")
    public <K> @Nullable Map<K, Object> getMap(@NotNull String arg, @NotNull Class<K> keyType) {
        Objects.requireNonNull(keyType, "keyType");

        return (Map<K, Object>) this.getMap(arg);
    }

    @SuppressWarnings("unchecked")
    public <K, V> @Nullable Map<K, V> getMap(@NotNull String arg, @NotNull Class<K> keyType, @NotNull Class<V> valueType) {
        Objects.requireNonNull(keyType, "keyType");
        Objects.requireNonNull(valueType, "valueType");

        return (Map<K, V>) this.getMap(arg);
    }

    public boolean isPresent(@NotNull String arg) {
        Objects.requireNonNull(arg, "arg");

        return this.args.containsKey(arg);
    }

    @UnmodifiableView
    public @NotNull Map<String, Object> getArgsView() {
        return Collections.unmodifiableMap(args);
    }

    public final void freeze() {
        ensureNotFrozen();
        this.frozen = true;
    }

    public final void ensureNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("This %s is frozen!".formatted(this.getClass().getSimpleName()));
        }
    }

    public final boolean isFrozen() {
        return frozen;
    }

    protected static Function<String, Object> flagArgTransformer() {
        return value -> value.isBlank() || Boolean.parseBoolean(value) || value.equalsIgnoreCase("1");
    }

    protected static <E extends Enum<E>> Function<String, Object> enumArgTransformer(Class<E> type) {
        return value -> Enum.valueOf(type, value.toUpperCase());
    }

    protected static Function<String, Object> notAllowedTransformer(String message) {
        return value -> {
            throw new IllegalArgumentException(message == null ? "Not allowed to set this via the cli" : message);
        };
    }

}
