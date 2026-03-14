package de.craftsblock.craftsnet.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public abstract class AbstractCraftsNetBuilder<T extends AbstractCraftsNetBuilder<T>> {

    private T owner;
    private final @NotNull CraftsNetBuilder parent;
    private boolean frozen = false;

    private final Map<String, String> args = new ConcurrentHashMap<>();
    private final Map<String, String> argsView = Collections.unmodifiableMap(args);

    public AbstractCraftsNetBuilder(@NotNull CraftsNetBuilder parent) {
        this.parent = parent;
    }

    protected final void acquire(T owner) {
        this.owner = owner;
    }

    public @NotNull T set(@NotNull String arg) {
        return this.set(arg, "");
    }

    public @NotNull T set(@NotNull String arg, @NotNull Object value) {
        return this.set(arg, value, Object::toString);
    }

    public <E> @NotNull T set(@NotNull String arg, @NotNull E value,
                              @NotNull Function<@NotNull E, @NotNull String> stringifier) {
        ensureNotFrozen();
        this.args.put(arg.toLowerCase(), stringifier.apply(value));
        return owner;
    }

    public String asString(@NotNull String arg) {
        return this.args.get(arg.toLowerCase());
    }

    public boolean asBoolean(@NotNull String arg) {
        return Boolean.parseBoolean(this.asString(arg));
    }

    public byte asByte(@NotNull String arg) {
        return Byte.parseByte(this.asString(arg));
    }

    public byte asByte(@NotNull String arg, int radix) {
        return Byte.parseByte(this.asString(arg), radix);
    }

    public short asShort(@NotNull String arg) {
        return Short.parseShort(this.asString(arg));
    }

    public short asShort(@NotNull String arg, int radix) {
        return Short.parseShort(this.asString(arg), radix);
    }

    public int asInt(@NotNull String arg) {
        return Integer.parseInt(this.asString(arg));
    }

    public int asInt(@NotNull String arg, int radix) {
        return Integer.parseInt(this.asString(arg), radix);
    }

    public long asLong(@NotNull String arg) {
        return Long.parseLong(this.asString(arg));
    }

    public long asLong(@NotNull String arg, int radix) {
        return Long.parseLong(this.asString(arg), radix);
    }

    public float asFloat(@NotNull String arg) {
        return Float.parseFloat(this.asString(arg));
    }

    public double asDouble(@NotNull String arg) {
        return Double.parseDouble(this.asString(arg));
    }

    public <E extends Enum<E>> E asEnum(@NotNull String arg, @NotNull Class<E> type) {
        return Enum.valueOf(type, this.asString(arg).toUpperCase());
    }

    public UUID asUuid(@NotNull String arg) {
        return UUID.fromString(this.asString(arg));
    }

    public boolean present(@NotNull String arg) {
        return this.args.containsKey(arg);
    }

    @UnmodifiableView
    public @NotNull Map<String, String> getAllArgs() {
        return argsView;
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

    public final @NotNull CraftsNetBuilder back() {
        return parent;
    }

}
