package de.craftsblock.craftsnet.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The Logger class provides a simple logging mechanism for displaying log messages in the console with different log levels.
 * It supports INFO, WARNING, ERROR, and DEBUG log levels. Log messages are displayed with colored output, and the logger can be
 * configured to include or exclude DEBUG messages based on the debug mode setting. Additionally, the class provides helper methods
 * to log error messages along with exception stack traces. The log messages include timestamps, log levels, thread names, and the
 * actual log text to help with debugging and tracking application behavior.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1
 * @since CraftsNet-1.0.0
 */
public class Logger {

    private final String name;
    private final boolean debug;

    /**
     * Constructs a Logger with the specified debug mode.
     *
     * @param debug If set to true, debug messages will be printed; otherwise, they will be ignored.
     */
    public Logger(boolean debug) {
        this(debug, null);
    }

    /**
     * Constructs a Logger with the specified debug mode and name.
     *
     * @param debug If set to true, debug messages will be printed; otherwise, they will be ignored.
     * @param name  The name of the logger, null if no name set
     */
    public Logger(boolean debug, @Nullable String name) {
        this.debug = debug;
        this.name = name;
    }

    /**
     * Logs an informational message.
     *
     * @param text The message to be logged.
     */
    public void info(@Nullable String text) {
        log("\u001b[34;1mINFO\u001b[0m ", text);
    }

    /**
     * Logs a warning message.
     *
     * @param text The warning message to be logged.
     */
    public void warning(@Nullable String text) {
        log("\u001b[33mWARN\u001b[0m ", text);
    }

    /**
     * Logs an error message.
     *
     * @param text The error message to be logged.
     */
    public void error(@Nullable String text) {
        log("\u001b[31;1mERROR\u001b[0m", text);
    }

    /**
     * Logs an error message along with the stack trace of an exception.
     *
     * @param throwable The throwable to be logged.
     */
    public void error(@NotNull Throwable throwable) {
        log("\u001b[31;1mERROR\u001b[0m", throwable.getMessage());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        for (String line : sw.toString().split("\n"))
            error(line);
    }

    /**
     * Logs an error message along with the stack trace of an exception and an additional comment.
     *
     * @param throwable The exception to be logged.
     * @param comment   An additional comment to be logged.
     */
    public void error(@NotNull Throwable throwable, @Nullable String comment) {
        log("\u001b[31;1mERROR\u001b[0m", comment + " > " + throwable.getMessage());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        for (String line : sw.toString().split("\n"))
            error(line);
    }

    /**
     * Logs a debug message. Debug messages are only printed if debug mode is enabled.
     *
     * @param text The debug message to be logged.
     */
    public void debug(@Nullable String text) {
        if (debug)
            log("\u001b[38;5;147mDEBUG\u001b[0m", text);
    }

    /**
     * Clone this instance of the {@link Logger} and set a custom name
     *
     * @param name The new name which should be used
     * @return A new instance of {@link Logger} with the new name set
     */
    public Logger cloneWithName(String name) {
        return new Logger(this.debug, name);
    }

    /**
     * Helper method to format and print the log message to the console.
     *
     * @param prefix The log level prefix (e.g., INFO, WARN, ERROR, DEBUG).
     * @param text   The log message to be printed.
     */
    private void log(@NotNull String prefix, @Nullable String text) {
        System.out.println(
                "\u001b[38;5;228m" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\u001b[0m " +
                        prefix + " \u001b[38;5;219m|\u001b[0m " +
                        "\u001b[36m" + (name != null ? name : Thread.currentThread().getName()) + "\u001b[0m" +
                        "\u001b[38;5;252m: " + text + "\u001b[0m"
        );
    }

}
