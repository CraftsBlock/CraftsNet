package de.craftsblock.craftsnet.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A logger interface for logging messages at different severity levels.
 * Implementations of this interface can be used for logging messages to various destinations
 * such as console, files, databases, etc.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0
 * @since CraftsNet-1.0.0
 */
public interface Logger {

    /**
     * Logs an informational message.
     *
     * @param text The text of the informational message to log. It can be {@code null}.
     */
    void info(@Nullable String text);

    /**
     * Logs a warning message.
     *
     * @param text The text of the warning message to log. It can be {@code null}.
     */
    void warning(@Nullable String text);

    /**
     * Logs an error message.
     *
     * @param text The text of the error message to log. It can be {@code null}.
     */
    void error(@Nullable String text);

    /**
     * Logs an error message along with the associated throwable.
     *
     * @param throwable The {@code Throwable} object representing the error.
     *                  Must not be {@code null}.
     */
    void error(@NotNull Throwable throwable);

    /**
     * Logs an error message along with the associated throwable and additional comment.
     *
     * @param throwable The {@code Throwable} object representing the error.
     *                  Must not be {@code null}.
     * @param comment   Additional comment or context for the error. It can be {@code null}.
     */
    void error(@NotNull Throwable throwable, @Nullable String comment);

    /**
     * Logs a debug message.
     *
     * @param text The text of the debug message to log. It can be {@code null}.
     */
    void debug(@Nullable String text);

    /**
     * Creates a clone of this logger with a specified name.
     *
     * @param name The name for the cloned logger.
     * @return A new {@code Logger} instance cloned from this logger with the given name.
     */
    Logger cloneWithName(String name);

}
