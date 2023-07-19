package de.craftsblock.craftsnet.utils;

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
 * @since 1.0.0
 */
public class Logger {

    private boolean debug;

    /**
     * Constructs a Logger with the specified debug mode.
     *
     * @param debug If set to true, debug messages will be printed; otherwise, they will be ignored.
     */
    public Logger(boolean debug) {
        this.debug = debug;
    }

    /**
     * Logs an informational message.
     *
     * @param text The message to be logged.
     */
    public void info(String text) {
        log("\u001b[34;1mINFO ", text);
    }

    /**
     * Logs a warning message.
     *
     * @param text The warning message to be logged.
     */
    public void warning(String text) {
        log("\u001b[33mWARN ", text);
    }

    /**
     * Logs an error message.
     *
     * @param text The error message to be logged.
     */
    public void error(String text) {
        log("\u001b[31;1mERROR", text);
    }

    /**
     * Logs an error message along with the stack trace of an exception.
     *
     * @param exception The exception to be logged.
     */
    public void error(Exception exception) {
        log("\u001b[31;1mERROR", exception.getMessage());
        exception.printStackTrace();
    }

    /**
     * Logs an error message along with the stack trace of an exception and an additional comment.
     *
     * @param exception The exception to be logged.
     * @param comment   An additional comment to be logged.
     */
    public void error(Exception exception, String comment) {
        log("\u001b[31;1mERROR", comment + " > " + exception.getMessage());
        exception.printStackTrace();
    }

    /**
     * Logs a debug message. Debug messages are only printed if debug mode is enabled.
     *
     * @param text The debug message to be logged.
     */
    public void debug(String text) {
        if (debug)
            log("\u001b[38;5;147mDEBUG", text);
    }

    /**
     * Helper method to format and print the log message to the console.
     *
     * @param prefix The log level prefix (e.g., INFO, WARN, ERROR, DEBUG).
     * @param text   The log message to be printed.
     */
    private void log(String prefix, String text) {
        System.out.println("\u001b[38;5;228m" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + prefix + " \u001b[38;5;219m| \u001b[36m" + Thread.currentThread().getName() + "\u001b[38;5;252m: " + text + "\u001b[0m");
    }

}
