package de.craftsblock.craftsnet.logging.impl;

import de.craftsblock.craftsnet.logging.LogLevel;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The LoggerImpl class provides a simple logging mechanism for displaying log messages in the console with different log levels.
 * It supports INFO, WARNING, ERROR, and DEBUG log levels. Log messages are displayed with colored output, and the logger can be
 * configured to include or exclude DEBUG messages based on the debug mode setting. Additionally, the class provides helper methods
 * to log error messages along with exception stack traces. The log messages include timestamps, log levels, thread names, and the
 * actual log text to help with debugging and tracking application behavior.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.0.5-SNAPSHOT
 */
public class LoggerImpl implements Logger {

    private final String name;
    private final boolean debug;

    /**
     * Constructs a Logger with the specified debug mode.
     *
     * @param debug If set to true, debug messages will be printed; otherwise, they will be ignored.
     */
    public LoggerImpl(boolean debug) {
        this(debug, null);
    }

    /**
     * Constructs a Logger with the specified debug mode and name.
     *
     * @param debug If set to true, debug messages will be printed; otherwise, they will be ignored.
     * @param name  The name of the logger, null if no name set
     */
    private LoggerImpl(boolean debug, @Nullable String name) {
        this.debug = debug;
        this.name = name;
    }

    /**
     * Logs an informational message.
     *
     * @param text The message to be logged.
     */
    @Override
    public void info(@Nullable String text) {
        log(LogLevel.INFO, text);
    }

    /**
     * Logs a warning message.
     *
     * @param text The warning message to be logged.
     */
    @Override
    public void warning(@Nullable String text) {
        log(LogLevel.WARNING, text);
    }

    /**
     * Logs an error message.
     *
     * @param text The error message to be logged.
     */
    @Override
    public void error(@Nullable String text) {
        log(LogLevel.ERROR, text);
    }

    /**
     * Logs an error message along with the stack trace of an exception.
     *
     * @param throwable The throwable to be logged.
     */
    @Override
    public void error(@NotNull Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        for (String line : sw.toString().split("\n"))
            log(LogLevel.EXCEPTION, line);
    }

    /**
     * Logs an error message along with the stack trace of an exception and an additional comment.
     *
     * @param throwable The exception to be logged.
     * @param comment   An additional comment to be logged.
     */
    @Override
    public void error(@NotNull Throwable throwable, @Nullable String comment) {
        log(LogLevel.EXCEPTION, comment);
        error(throwable);
    }

    /**
     * Logs a debug message. Debug messages are only printed if debug mode is enabled.
     *
     * @param text The debug message to be logged.
     */
    @Override
    public void debug(@Nullable String text) {
        if (debug)
            log(LogLevel.DEBUG, text);
    }

    /**
     * Clone this instance of the {@link Logger} and set a custom name
     *
     * @param name The new name which should be used
     * @return A new instance of {@link Logger} with the new name set
     */
    @Override
    public Logger cloneWithName(String name) {
        return new LoggerImpl(this.debug, name);
    }

    /**
     * Helper method to format and print the log message to the console.
     *
     * @param level The log level (e.g., INFO, WARN, ERROR, DEBUG).
     * @param text  The log message to be printed.
     */
    private void log(@NotNull LogLevel level, @Nullable String text) {
        System.out.println(
                "\u001b[38;5;228m" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\u001b[0m " +
                        level.getPrefix() + "\u001b[38;5;219m|\u001b[0m " +
                        "\u001b[36m" + (name != null ? name : Thread.currentThread().getName().replaceFirst("CraftsNet(\\s+)?", "")) +
                        "\u001b[0m\u001b[38;5;252m: " + text + "\u001b[0m"
        );
    }

}
