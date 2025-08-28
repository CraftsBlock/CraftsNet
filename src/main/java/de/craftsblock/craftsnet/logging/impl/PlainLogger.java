package de.craftsblock.craftsnet.logging.impl;

import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the {@link Logger} interface that prints the result
 * in the {@link System#out} or the {@link System#err} stream, without any
 * further formating.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @since 3.1.0-SNAPSHOT
 */
public record PlainLogger(Logger previous) implements Logger {

    /**
     * Constructs an {@link PlainLogger} with a reference to a previous {@link Logger}.
     *
     * @param previous the previous {@link Logger} in the chain, can be used for chaining or fallback purposes.
     */
    public PlainLogger {
    }

    /**
     * Returns the previous {@link Logger} in the chain.
     * This can be used to access the logger that was previously used before this {@link PlainLogger}.
     *
     * @return The previous {@link Logger} instance.
     */
    public Logger previous() {
        return this.previous;
    }

    /**
     * {@inheritDoc}
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void info(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * {@inheritDoc}
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void warning(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * {@inheritDoc}
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void error(@Nullable String text) {
        System.err.println(text);
    }

    /**
     * {@inheritDoc}
     *
     * @param throwable {@inheritDoc}
     */
    @Override
    public void error(@NotNull Throwable throwable) {
        throwable.printStackTrace(System.err);
    }

    /**
     * {@inheritDoc}
     *
     * @param message {@inheritDoc}
     * @param throwable {@inheritDoc}
     * @since 3.5.2
     */
    @Override
    public void error(@Nullable String message, @NotNull Throwable throwable) {
        System.err.println(message);
        throwable.printStackTrace(System.err);
    }

    /**
     * {@inheritDoc}
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void debug(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * {@inheritDoc}
     *
     * @param name {@inheritDoc} (Not used in any way!)
     * @return {@inheritDoc}
     */
    @Override
    public Logger cloneWithName(String name) {
        return this;
    }

}
