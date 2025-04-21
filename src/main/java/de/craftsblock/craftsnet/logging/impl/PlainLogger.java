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
 * @version 1.0.0
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
     * @return the previous {@link Logger} instance.
     */
    public Logger previous() {
        return this.previous;
    }

    /**
     * Logs an informational message.
     *
     * @param text the informational message to be logged, can be null.
     */
    @Override
    public void info(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * Logs a warning message.
     *
     * @param text the warning message to be logged, can be null.
     */
    @Override
    public void warning(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * Logs an error message.
     *
     * @param text the error message to be logged, can be null.
     */
    @Override
    public void error(@Nullable String text) {
        System.err.println(text);
    }

    /**
     * Logs an error with a throwable.
     *
     * @param throwable the {@link Throwable} to be logged, must not be null.
     */
    @Override
    public void error(@NotNull Throwable throwable) {
        throwable.printStackTrace(System.err);
    }

    /**
     * Logs an error with a throwable and an additional comment.
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
     *
     * @param text the debug message to be logged, can be null.
     */
    @Override
    public void debug(@Nullable String text) {
        System.out.println(text);
    }

    /**
     * Returns this instance of the {@link PlainLogger}.
     *
     * @param name the name for the new logger instance. (Not used in any way!)
     * @return the current instance of this {@link PlainLogger}.
     */
    @Override
    public Logger cloneWithName(String name) {
        return this;
    }

}
