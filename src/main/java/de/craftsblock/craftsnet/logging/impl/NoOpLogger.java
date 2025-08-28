package de.craftsblock.craftsnet.logging.impl;

import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the {@link Logger} interface that performs no operations.
 * The {@link NoOpLogger} simply accepts log messages and errors but does not process
 * them in any way.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.1
 * @since 3.4.0-SNAPSHOT
 */
public record NoOpLogger(Logger previous) implements Logger {

    /**
     * Constructs an {@link NoOpLogger} with a reference to a previous {@link Logger}.
     *
     * @param previous the previous {@link Logger} in the chain, can be used for chaining or fallback purposes.
     */
    public NoOpLogger {
    }

    /**
     * {@inheritDoc}
     * This implementation does nothing.
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void info(@Nullable String text) {
    }

    /**
     * {@inheritDoc}
     * This implementation does nothing.
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void warning(@Nullable String text) {
    }

    /**
     * {@inheritDoc}
     * This implementation does nothing.
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void error(@Nullable String text) {
    }

    /**
     * {@inheritDoc}
     * This implementation does nothing.
     *
     * @param throwable {@inheritDoc}
     */
    @Override
    public void error(@NotNull Throwable throwable) {
    }

    /**
     * {@inheritDoc}
     * This implementation does nothing.
     *
     * @param message   {@inheritDoc}
     * @param throwable {@inheritDoc}
     */
    @Override
    public void error(@Nullable String message, @NotNull Throwable throwable) {
    }

    /**
     * {@inheritDoc}
     * This implementation does nothing.
     *
     * @param text {@inheritDoc}
     */
    @Override
    public void debug(@Nullable String text) {
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

    /**
     * Utility method for formatting a string with arguments.
     * <p>
     * This method just returns the unformatted format as this
     * logger has no actions.
     *
     * @param format {@inheritDoc}
     * @param args   {@inheritDoc}
     * @return {@inheritDoc}
     * @since 3.5.2
     */
    @Override
    public String format(@NotNull String format, Object... args) {
        return format;
    }
}
