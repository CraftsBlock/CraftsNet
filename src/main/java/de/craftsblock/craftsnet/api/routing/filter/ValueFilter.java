package de.craftsblock.craftsnet.api.routing.filter;

import de.craftsblock.craftsnet.api.Exchange;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class ValueFilter<E extends Exchange, T> extends Filter<E> {

    private final @NotNull T value;
    private final boolean distinguishing;

    protected ValueFilter(@NotNull T value) {
        this(value, false);
    }

    protected ValueFilter(@NotNull T value, boolean distinguishing) {
        this.value = value;
        this.distinguishing = distinguishing;
    }

    public @NotNull T getValue() {
        return value;
    }

    @Override
    protected boolean canDistinguish() {
        return distinguishing;
    }

    @Override
    public final boolean equals(Filter<?> that) {
        return this.equals((ValueFilter<?, ?>) that);
    }

    public boolean equals(ValueFilter<?, ?> that) {
        return this.getValue().equals(that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

}
