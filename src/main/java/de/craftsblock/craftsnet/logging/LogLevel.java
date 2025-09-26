package de.craftsblock.craftsnet.logging;

/**
 * An enumeration of log levels used for categorizing log messages.
 * Each log level has an associated color-coded prefix for formatting purposes.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.1.0-SNAPSHOT
 */
public enum LogLevel {

    /**
     * Debug level for detailed debugging information.
     * Color-coded light blue.
     */
    DEBUG("\u001b[38;5;147mDEBUG\t\u001b[0m"),

    /**
     * Informational level for general information messages.
     * Color-coded blue.
     */
    INFO("\u001b[34;1mINFO\t\u001b[0m"),

    /**
     * Warning level for potential issues or important notices.
     * Color-coded yellow.
     */
    WARNING("\u001b[33mWARN\t\u001b[0m"),

    /**
     * Error level for error messages indicating problems.
     * Color-coded red.
     */
    ERROR("\u001b[31;1mERROR\t\u001b[0m"),

    /**
     * Exception level for fatal errors and exceptions.
     * Color-coded red.
     */
    EXCEPTION("\u001b[31;1mFATAL\t\u001b[0m");

    private final String prefix;

    /**
     * Constructs a {@code LogLevel} with a specified color-coded prefix.
     *
     * @param prefix the color-coded prefix associated with the log level.
     */
    LogLevel(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the color-coded prefix for this log level.
     *
     * @return the prefix associated with this log level.
     */
    public String getPrefix() {
        return prefix;
    }

}
