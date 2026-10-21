package de.craftsblock.craftsnet.api.routing.filter;

import de.craftsblock.craftsnet.api.Exchange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Filter<E extends Exchange> {

    public abstract @Nullable Object filter(@NotNull E exchange, @NotNull FilterChain<E> chain);

    protected boolean canDistinguish() {
        return false;
    }

    protected boolean overlaps(Filter<?> that) {
        return that != null && this.getClass() == that.getClass();
    }

    @Override
    public final boolean equals(Object that) {
        if (that == null || this.getClass() != that.getClass()) {
            return false;
        }

        return this.equals((Filter<?>) that);
    }

    public boolean equals(Filter<?> that) {
        return false;
    }

}
