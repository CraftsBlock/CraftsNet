package de.craftsblock.craftsnet.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Function;

/**
 * Base implementation for builders within the CraftsNet builder system.
 * <p>
 * This class provides a common mechanism for storing, transforming and retrieving
 * named arguments. It also manages the association between a builder and its
 * owning builder, as well as controlling the builder's mutability through a
 * freezing mechanism.
 * <p>
 * Arguments are identified by their names, which are normalized to lowercase
 * before being stored or retrieved. Known arguments can be registered with
 * individual transformation functions that are applied to string values before
 * they are stored. Unknown arguments can optionally be allowed depending on
 * the builder configuration.
 * <p>
 * The self-referencing generic type {@code T} allows builder operations to
 * return the concrete builder owner, enabling fluent method chaining across
 * different builder implementations.
 *
 * @param <T> The concrete builder type that owns this builder instance
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 4.0.0
 */
public abstract class AbstractCraftsNetBuilder<T extends AbstractCraftsNetBuilder<T>> {

    private T owner;
    private boolean frozen = false;

    private final Map<String, Object> args = new HashMap<>();
    private final Map<String, Function<String, Object>> argTransformers = new HashMap<>();

    private final boolean allowUnknownArgs;

    /**
     * Creates a new builder that does not allow unknown arguments.
     * <p>
     * All arguments must be registered before they can be assigned.
     */
    protected AbstractCraftsNetBuilder() {
        this(false);
    }

    /**
     * Creates a new builder with the specified unknown-argument handling policy.
     *
     * @param allowUnknownArgs {@code true} to allow arguments that have not
     *                         been explicitly registered, {@code false} otherwise
     */
    protected AbstractCraftsNetBuilder(boolean allowUnknownArgs) {
        this.allowUnknownArgs = allowUnknownArgs;
    }

    /**
     * Registers a known argument using the default identity transformer.
     * <p>
     * The identity transformer leaves string values unchanged.
     *
     * @param arg The name of the argument to register
     * @throws NullPointerException  If {@code arg} is {@code null}
     * @throws IllegalStateException If this builder has already been frozen
     */
    protected void addKnownArgument(@NotNull String arg) {
        this.addKnownArgument(arg, ArgumentTransformer.identity());
    }

    /**
     * Registers a known argument with a custom transformation function.
     * <p>
     * The argument name is normalized to lowercase before registration.
     * The supplied transformer is subsequently applied to string values
     * assigned to this argument through {@link #set(String, Object)}.
     *
     * @param arg         The name of the argument to register
     * @param transformer The transformation function used to process string values
     * @throws NullPointerException  If {@code arg} or {@code transformer} is {@code null}
     * @throws IllegalStateException If this builder has already been frozen
     */
    protected void addKnownArgument(@NotNull String arg, @NotNull Function<String, Object> transformer) {
        Objects.requireNonNull(arg, "arg");
        Objects.requireNonNull(transformer, "transformer");

        ensureNotFrozen();
        this.argTransformers.put(arg.toLowerCase(), transformer);
    }

    /**
     * Associates this builder with its owning root builder.
     * <p>
     * A builder can only be acquired once. Attempting to assign another owner
     * after an owner has already been set results in an exception.
     *
     * @param owner The root builder that owns this builder
     * @throws NullPointerException  If {@code owner} is {@code null}
     * @throws IllegalStateException If this builder is frozen or already has an owner
     */
    protected final void acquire(@NotNull T owner) {
        ensureNotFrozen();

        if (this.owner != null) {
            throw new IllegalStateException("The builder is already known by a root");
        }

        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /**
     * Assigns an empty string to the specified argument.
     * <p>
     * This overload is intended for arguments that do not require an explicit
     * value, such as flags or presence-based options.
     *
     * @param arg The name of the argument to assign
     * @return The owning root builder for fluent method chaining
     * @throws NullPointerException  If {@code arg} is {@code null}
     * @throws IllegalStateException If this builder is frozen or the argument
     *                               is unknown and unknown arguments are disabled
     */
    protected @NotNull T set(@NotNull String arg) {
        return this.set(arg, "");
    }

    /**
     * Assigns a value to the specified argument.
     * <p>
     * The argument name is normalized to lowercase before being stored.
     * If a transformer is registered for the argument and the supplied value
     * is a string, the transformer is applied before the value is stored.
     * Non-string values are stored without transformation.
     * <p>
     * If no transformer is registered, the identity transformer is used.
     * Unknown arguments are only accepted when {@code allowUnknownArgs}
     * is enabled.
     *
     * @param arg   The name of the argument to assign
     * @param value The value to associate with the argument
     * @return The owning root builder for fluent method chaining
     * @throws NullPointerException     If {@code arg} or {@code value} is {@code null}
     * @throws IllegalArgumentException If the argument is unknown and unknown
     *                                  arguments are disabled
     * @throws IllegalStateException    If this builder has already been frozen
     */
    protected @NotNull T set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");
        Objects.requireNonNull(value, "value");

        ensureNotFrozen();

        if (!argTransformers.containsKey(arg.toLowerCase()) && !allowUnknownArgs) {
            throw new IllegalArgumentException("Can not handle unknown argument: " + arg);
        }

        Function<String, Object> argumentTransformer = this.argTransformers.getOrDefault(
                arg.toLowerCase(),
                ArgumentTransformer.identity()
        );

        Object transformed;
        if (value instanceof String string && !argumentTransformer.equals(ArgumentTransformer.identity())) {
            transformed = argumentTransformer.apply(string);
        } else {
            transformed = value;
        }

        this.args.put(arg.toLowerCase(), transformed);
        return owner;
    }

    /**
     * Retrieves the value associated with the specified argument.
     * <p>
     * The argument name is normalized to lowercase before the lookup.
     *
     * @param arg The name of the argument to retrieve
     * @return The associated value, or {@code null} if the argument has not
     * been assigned a value
     * @throws NullPointerException If {@code arg} is {@code null}
     */
    public @Nullable Object get(@NotNull String arg) {
        Objects.requireNonNull(arg, "arg");

        return this.args.get(arg.toLowerCase());
    }

    /**
     * Retrieves the value of the specified argument and casts it to the
     * requested type.
     * <p>
     * This method performs an unchecked cast and does not verify whether the
     * stored value is actually compatible with the supplied type.
     *
     * @param <E>  The expected type of the argument value
     * @param arg  The name of the argument to retrieve
     * @param type The expected class of the argument value
     * @return The stored value cast to {@code E}, or {@code null} if the
     * argument has not been assigned a value
     * @throws NullPointerException If {@code arg} or {@code type} is {@code null}
     * @throws ClassCastException   If the stored value cannot be cast to {@code E}
     */
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

    /**
     * Retrieves the specified argument as a string.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as a {@link String}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link String}
     */
    public @Nullable String getString(@NotNull String arg) {
        return this.getTyped(arg, String.class);
    }

    /**
     * Retrieves the specified argument as a boolean value.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as a {@link Boolean}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Boolean}
     */
    public @Nullable Boolean getBoolean(@NotNull String arg) {
        return this.getTyped(arg, Boolean.class);
    }

    /**
     * Retrieves the specified argument as a byte value.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as a {@link Byte}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Byte}
     */
    public @Nullable Byte getByte(@NotNull String arg) {
        return this.getTyped(arg, Byte.class);
    }

    /**
     * Retrieves the specified argument as a short value.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as a {@link Short}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Short}
     */
    public @Nullable Short getShort(@NotNull String arg) {
        return this.getTyped(arg, Short.class);
    }

    /**
     * Retrieves the specified argument as an integer value.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as an {@link Integer}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not an {@link Integer}
     */
    public @Nullable Integer getInt(@NotNull String arg) {
        return this.getTyped(arg, Integer.class);
    }

    /**
     * Retrieves the specified argument as a long value.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as a {@link Long}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Long}
     */
    public @Nullable Long getLong(@NotNull String arg) {
        return this.getTyped(arg, Long.class);
    }

    /**
     * Retrieves the specified argument as a float value.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as a {@link Float}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Float}
     */
    public @Nullable Float getFloat(@NotNull String arg) {
        return this.getTyped(arg, Float.class);
    }

    /**
     * Retrieves the specified argument as a double value.
     *
     * @param arg The name of the argument to retrieve
     * @return The argument value as a {@link Double}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Double}
     */
    public @Nullable Double getDouble(@NotNull String arg) {
        return this.getTyped(arg, Double.class);
    }

    /**
     * Retrieves the specified argument as an enum constant.
     * <p>
     * The returned value is cast to the requested enum type. No conversion
     * between strings and enum constants is performed.
     *
     * @param <E>  The enum type
     * @param arg  The name of the argument to retrieve
     * @param type The enum class used as the expected type
     * @return The stored enum constant, or {@code null} if no value is assigned
     * @throws NullPointerException If {@code arg} or {@code type} is {@code null}
     * @throws ClassCastException   If the stored value is not compatible with the
     *                              requested enum type
     */
    public <E extends Enum<E>> @Nullable E getEnum(@NotNull String arg, @NotNull Class<E> type) {
        return this.getTyped(arg, type);
    }

    /**
     * Retrieves the specified argument as a collection.
     *
     * @param arg The name of the argument to retrieve
     * @return The stored value as a {@link Collection}, or {@code null} if no
     * value is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Collection}
     */
    public @Nullable Collection<?> getCollection(@NotNull String arg) {
        return this.getTyped(arg, Collection.class);
    }

    /**
     * Retrieves the specified argument as a collection with the expected
     * element type.
     * <p>
     * The supplied element type is checked for {@code null}, but the actual
     * elements of the collection are not validated.
     *
     * @param <E>     The expected element type
     * @param arg     The name of the argument to retrieve
     * @param subType The expected class of the collection elements
     * @return The stored value cast to a collection of {@code E}, or
     * {@code null} if no value is assigned
     * @throws NullPointerException If {@code subType} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Collection}
     */
    @SuppressWarnings("unchecked")
    public <E> @Nullable Collection<E> getCollection(@NotNull String arg, @NotNull Class<E> subType) {
        Objects.requireNonNull(subType, "subType");

        return (Collection<E>) getCollection(arg);
    }

    /**
     * Retrieves the specified argument as a map.
     *
     * @param arg The name of the argument to retrieve
     * @return The stored value as a {@link Map}, or {@code null} if no value
     * is assigned
     * @throws NullPointerException If {@code arg} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Map}
     */
    public @Nullable Map<?, ?> getMap(@NotNull String arg) {
        return this.getTyped(arg, Map.class);
    }

    /**
     * Retrieves the specified argument as a map with the expected key type.
     * <p>
     * The supplied key type is checked for {@code null}, but the actual keys
     * and values contained in the map are not validated.
     *
     * @param <K>     The expected key type
     * @param arg     The name of the argument to retrieve
     * @param keyType The expected class of the map keys
     * @return The stored value cast to a map with keys of type {@code K} and
     * values of type {@link Object}, or {@code null} if no value is assigned
     * @throws NullPointerException If {@code keyType} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Map}
     */
    @SuppressWarnings("unchecked")
    public <K> @Nullable Map<K, Object> getMap(@NotNull String arg, @NotNull Class<K> keyType) {
        Objects.requireNonNull(keyType, "keyType");

        return (Map<K, Object>) this.getMap(arg);
    }

    /**
     * Retrieves the specified argument as a map with the expected key and
     * value types.
     * <p>
     * The supplied types are checked for {@code null}, but the actual keys
     * and values contained in the map are not validated.
     *
     * @param <K>       The expected key type
     * @param <V>       The expected value type
     * @param arg       The name of the argument to retrieve
     * @param keyType   The expected class of the map keys
     * @param valueType The expected class of the map values
     * @return The stored value cast to a map with keys of type {@code K} and
     * values of type {@code V}, or {@code null} if no value is assigned
     * @throws NullPointerException If {@code keyType} or {@code valueType} is {@code null}
     * @throws ClassCastException   If the stored value is not a {@link Map}
     */
    @SuppressWarnings("unchecked")
    public <K, V> @Nullable Map<K, V> getMap(@NotNull String arg, @NotNull Class<K> keyType, @NotNull Class<V> valueType) {
        Objects.requireNonNull(keyType, "keyType");
        Objects.requireNonNull(valueType, "valueType");

        return (Map<K, V>) this.getMap(arg);
    }

    /**
     * Checks whether the specified argument has been assigned a value.
     * <p>
     * Unlike {@link #get(String)}, this method distinguishes between an
     * argument that is absent and an argument that has explicitly been
     * assigned a {@code null} value. However, the {@link #set(String, Object)}
     * method does not permit null values.
     *
     * @param arg The name of the argument to check
     * @return {@code true} if the argument is present in the argument map,
     * {@code false} otherwise
     * @throws NullPointerException If {@code arg} is {@code null}
     */
    public boolean isPresent(@NotNull String arg) {
        Objects.requireNonNull(arg, "arg");

        return this.args.containsKey(arg);
    }

    /**
     * Returns an unmodifiable view of the arguments currently stored in this
     * builder.
     * <p>
     * The returned map is backed by the builder's internal argument map.
     * Consequently, changes made to the builder's arguments are reflected in
     * the view, but callers cannot modify the map through the returned reference.
     *
     * @return An unmodifiable view of the builder's argument map
     */
    @UnmodifiableView
    public @NotNull Map<String, Object> getArgsView() {
        return Collections.unmodifiableMap(args);
    }

    /**
     * Checks whether this builder permits arguments that have not been
     * explicitly registered.
     *
     * @return {@code true} if unknown arguments are allowed, {@code false} otherwise
     */
    public boolean isAllowUnknownArgs() {
        return allowUnknownArgs;
    }

    /**
     * Freezes this builder, preventing subsequent modifications.
     * <p>
     * Once frozen, operations that call {@link #ensureNotFrozen()} will fail
     * with an {@link IllegalStateException}. A frozen builder cannot be
     * unfrozen through this class.
     *
     * @throws IllegalStateException If this builder has already been frozen
     */
    public final void freeze() {
        ensureNotFrozen();
        this.frozen = true;
    }

    /**
     * Verifies that this builder has not been frozen.
     * <p>
     * This method is intended to be called before operations that modify
     * the builder's state.
     *
     * @throws IllegalStateException If this builder has already been frozen
     */
    public final void ensureNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("This %s is frozen!".formatted(this.getClass().getSimpleName()));
        }
    }

    /**
     * Checks whether this builder has been frozen.
     *
     * @return {@code true} if this builder is frozen, {@code false} otherwise
     */
    public final boolean isFrozen() {
        return frozen;
    }

}
