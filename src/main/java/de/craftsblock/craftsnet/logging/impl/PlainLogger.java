package de.craftsblock.craftsnet.logging.impl;

import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the {@link Logger} interface that performs no operations.
 * This can be used as a placeholder or default logger where logging functionality
 * is not required or should be ignored.
 * <p>
 * The {@code PlainLogger} simply accepts log messages and errors but does not process
 * them in any way. It is a no-op logger.
 * </p>
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.1.0-SNAPSHOT
 */
public record PlainLogger(Logger previous) implements Logger {

    /**
     * Constructs an {@code PlainLogger} with a reference to a previous {@code Logger}.
     *
     * @param previous the previous {@code Logger} in the chain, can be used for chaining or fallback purposes.
     */
    public PlainLogger {
    }

    /**
     * Returns the previous {@code Logger} in the chain.
     * This can be used to access the logger that was previously used before this {@code PlainLogger}.
     *
     * @return the previous {@code Logger} instance.
     */
    public Logger previous() {
        return this.previous;
    }

    /**
     * Logs an informational message.
     * This implementation does nothing.
     *
     * @param text the informational message to be logged, can be null.
     */
    @Override
    public void info(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * Logs a warning message.
     * This implementation does nothing.
     *
     * @param text the warning message to be logged, can be null.
     */
    @Override
    public void warning(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * Logs an error message.
     * This implementation does nothing.
     *
     * @param text the error message to be logged, can be null.
     */
    @Override
    public void error(@Nullable String text) {
        System.err.println(text);
    }

    /**
     * Logs an error with a throwable.
     * This implementation does nothing.
     *
     * @param throwable the {@link Throwable} to be logged, must not be null.
     */
    @Override
    public void error(@NotNull Throwable throwable) {
        throwable.printStackTrace(System.err);
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
        System.err.println(comment);
        throwable.printStackTrace(System.err);
    }

    /**
     * Logs a debug message.
     * This implementation does nothing.
     *
     * @param text the debug message to be logged, can be null.
     */
    @Override
    public void debug(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * Creates a new instance of {@code PlainLogger} with a specified name.
     * This implementation ignores the provided name and returns a new {@code PlainLogger}
     * with the same previous logger.
     *
     * @param name the name for the new logger instance.
     * @return a new instance of {@code PlainLogger} with the same previous logger.
     */
    @Override
    public Logger cloneWithName(String name) {
        return this;
    }

}
