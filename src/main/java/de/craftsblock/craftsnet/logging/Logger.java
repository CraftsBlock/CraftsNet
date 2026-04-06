package de.craftsblock.craftsnet.logging;

import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A logger interface for logging messages at different severity levels.
 * Implementations of this interface can be used for logging messages to various destinations
 * such as console, files, databases, etc.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 1.0.0-SNAPSHOT
 */
public interface Logger {

    /**
     * Logs an informational message.
     *
     * @param text The text of the informational message to log. It can be {@code null}.
     */
    void info(@Nullable String text);

    /**
     * Logs a formatted informational message.
     * <p>
     * This method formats the given string with the specified arguments
     * using {@link #format(String, Object...)} before logging it.
     *
     * @param format The format string. Must not be {@code null}.
     * @param args   Arguments referenced by the format specifiers in the format string.
     * @since 3.5.2
     */
    default void info(@NotNull @PrintFormat String format, Object @Nullable ... args) {
        info(format(format, args));
    }

    /**
     * Logs a warning message.
     *
     * @param text The text of the warning message to log. It can be {@code null}.
     */
    void warning(@Nullable String text);

    /**
     * Logs a formatted warning message.
     * <p>
     * This method formats the given string with the specified arguments
     * using {@link #format(String, Object...)} before logging it.
     *
     * @param format The format string. Must not be {@code null}.
     * @param args   Arguments referenced by the format specifiers in the format string.
     * @since 3.5.2
     */
    default void warning(@NotNull @PrintFormat String format, Object @Nullable ... args) {
        warning(format(format, args));
    }

    /**
     * Logs an error message.
     *
     * @param text The text of the error message to log. It can be {@code null}.
     */
    void error(@Nullable String text);

    /**
     * Logs a formatted error message.
     * <p>
     * This method formats the given string with the specified arguments
     * using {@link #format(String, Object...)} before logging it.
     *
     * @param format The format string. Must not be {@code null}.
     * @param args   Arguments referenced by the format specifiers in the format string.
     * @since 3.5.2
     */
    default void error(@NotNull @PrintFormat String format, Object @Nullable ... args) {
        error(format(format, args));
    }

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
     * @deprecated Use {@link #error(String, Throwable)}
     */
    @Deprecated(since = "3.5.2", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
    default void error(@NotNull Throwable throwable, @Nullable String comment) {
        error(comment, throwable);
    }

    /**
     * Logs an error message along with the associated throwable.
     *
     * @param message   A descriptive message for the error. It can be {@code null}.
     * @param throwable The {@code Throwable} object representing the error. Must not be {@code null}.
     * @since 3.5.2
     */
    void error(@Nullable String message, @NotNull Throwable throwable);

    /**
     * Logs a formatted error message along with the associated throwable.
     * <p>
     * This method formats the given string with the specified arguments
     * using {@link #format(String, Object...)} before logging it together
     * with the provided throwable.
     *
     * @param format    The format string. Must not be {@code null}.
     * @param throwable The {@code Throwable} object representing the error. Must not be {@code null}.
     * @param args      Arguments referenced by the format specifiers in the format string.
     * @since 3.5.2
     */
    default void error(@NotNull @PrintFormat String format, @NotNull Throwable throwable,
                       Object @Nullable ... args) {
        error(format(format, args), throwable);
    }

    /**
     * Logs a debug message.
     *
     * @param text The text of the debug message to log. It can be {@code null}.
     */
    void debug(@Nullable String text);

    /**
     * Logs a formatted debug message.
     * <p>
     * This method formats the given string with the specified arguments
     * using {@link #format(String, Object...)} before logging it.
     *
     * @param format The format string. Must not be {@code null}.
     * @param args   Arguments referenced by the format specifiers in the format string.
     * @since 3.5.2
     */
    default void debug(@NotNull @PrintFormat String format, Object @Nullable ... args) {
        debug(format(format, args));
    }

    /**
     * Creates a clone of this {@link Logger} with a specified name.
     *
     * @param name The name for the cloned logger.
     * @return A new {@code Logger} instance cloned from this logger with the given name.
     */
    Logger cloneWithName(@Nullable String name);

    /**
     * Creates a clone of this {@link Logger} with a formatted name.
     * <p>
     * The provided format string and arguments are combined into a name
     * using {@link #format(String, Object...)}, which is then used
     * to create the cloned logger.
     *
     * @param format The format string used for the logger name. Must not be {@code null}.
     * @param args   Arguments referenced by the format specifiers in the format string.
     * @return A new {@code Logger} instance cloned from this logger with the formatted name.
     * @since 3.5.2
     */
    default Logger cloneWithName(@NotNull @PrintFormat String format, Object @Nullable ... args) {
        return cloneWithName(format(format, args));
    }

    /**
     * Utility method for formatting a string with arguments.
     * <p>
     * This method simply delegates to {@link String#format(String, Object...)}.
     *
     * @param format The format string. Must not be {@code null}.
     * @param args   Arguments referenced by the format specifiers in the format string.
     * @return A formatted string.
     * @since 3.5.2
     */
    default String format(@NotNull @PrintFormat String format, Object @Nullable ... args) {
        return String.format(format, args);
    }

}
