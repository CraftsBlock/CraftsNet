package de.craftsblock.craftsnet.logging.impl;

import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the {@link Logger} interface that performs no operations.
 * The {@link NoLogLogger} simply accepts log messages and errors but does not process
 * them in any way.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.3.6-SNAPSHOT
 */
public record NoLogLogger(Logger previous) implements Logger {

    /**
     * Constructs an {@link NoLogLogger} with a reference to a previous {@link Logger}.
     *
     * @param previous the previous {@link Logger} in the chain, can be used for chaining or fallback purposes.
     */
    public NoLogLogger {
    }

    /**
     * Logs an informational message.
     * This implementation does nothing.
     *
     * @param text the informational message to be logged, can be null.
     */
    @Override
    public void info(@Nullable String text) {
    }

    /**
     * Logs a warning message.
     * This implementation does nothing.
     *
     * @param text the warning message to be logged, can be null.
     */
    @Override
    public void warning(@Nullable String text) {
    }

    /**
     * Logs an error message.
     * This implementation does nothing.
     *
     * @param text the error message to be logged, can be null.
     */
    @Override
    public void error(@Nullable String text) {
    }

    /**
     * Logs an error with a throwable.
     * This implementation does nothing.
     *
     * @param throwable the {@link Throwable} to be logged, must not be null.
     */
    @Override
    public void error(@NotNull Throwable throwable) {
    }

    /**
     * Logs an error with a throwable and an additional comment.
     * This implementation does nothing.
     *
     * @param throwable the {@link Throwable} to be logged, must not be null.
     * @param comment   an additional comment or message to be logged, can be null.
     */
    @Override
    public void error(@NotNull Throwable throwable, @Nullable String comment) {
    }

    /**
     * Logs a debug message.
     * This implementation does nothing.
     *
     * @param text the debug message to be logged, can be null.
     */
    @Override
    public void debug(@Nullable String text) {
    }

    /**
     * Returns this instance of the {@link NoLogLogger}.
     *
     * @param name the name for the new logger instance. (Not used in any way!)
     * @return the current instance of this {@link NoLogLogger}.
     */
    @Override
    public Logger cloneWithName(String name) {
        return this;
    }

}
