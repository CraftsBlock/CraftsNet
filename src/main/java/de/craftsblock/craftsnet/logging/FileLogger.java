package de.craftsblock.craftsnet.logging;

import de.craftsblock.craftscore.id.Snowflake;
import de.craftsblock.craftsnet.CraftsNet;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A utility class for logging to files.
 * This class provides methods to log messages to files, handle error logs, and redirect standard output and error streams to log files.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.2
 */
public class FileLogger {

    private static final File folder = new File("logs");
    private static FileOutputStream stream;

    /**
     * Starts logging to files.
     * This method sets up file logging by redirecting standard output and error streams to log files.
     */
    public static void start() {
        try {
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;

            File file = new File(folder, "latest.log");
            if (file.exists()) {
                int i = 1;
                while (true) {
                    File newName = new File(folder, OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "_" + i + ".log");
                    if (newName.exists()) {
                        i++;
                        continue;
                    }
                    file.renameTo(newName);
                    start();
                    return;
                }
            }

            ensureParentFolder(file);
            file.createNewFile();

            stream = new FileOutputStream(file);
            System.setOut(new LoggerPrintStream(oldOut, stream));
            System.setErr(new LoggerPrintStream(oldErr, stream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a line to the log file.
     * This method adds a line to the current log file.
     *
     * @param line The line to add to the log file.
     */
    public static void addLine(String line) {
        if (stream == null) return;
        if (line != null && line.contains("SLF4J")) return;
        try {
            stream.write((LoggerPrintStream.removeAsciiColors(line) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            stream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an error log file for the given exception.
     * This method creates an error log file containing the stack trace of the given exception.
     *
     * @param exception The exception for which to create the error log file.
     * @param protocol  The protocol associated with the error.
     * @param path      The path associated with the error.
     * @return The identifier of the error log file.
     */
    public static long createErrorLog(Exception exception, String protocol, String path) {
        return createErrorLog(exception, Map.of("Protocol", protocol, "Path", path));
    }

    /**
     * Creates an error log file for the given throwable.
     * This method creates an error log file containing the stack trace of the given throwable.
     *
     * @param throwable The throwable for which to create the error log file.
     * @return The identifier of the error log file.
     */
    public static long createErrorLog(Throwable throwable) {
        return createErrorLog(throwable, Collections.emptyMap());
    }

    /**
     * Creates an error log file for the given throwable.
     * This method creates an error log file containing the stack trace of the given throwable, along with additional information.
     *
     * @param throwable  The throwable for which to create the error log file.
     * @param additional Additional information to include in the error log file.
     * @return The identifier of the error log file.
     */
    public static long createErrorLog(Throwable throwable, Map<String, String> additional) {
        File errors = new File(folder, "errors");
        ensureParentFolder(errors);
        if (!errors.exists()) errors.mkdirs();

        long identifier = Snowflake.generate();
        File errorFile = new File(errors, "error_" + identifier + ".log");
        try (PrintStream stream = new PrintStream(new FileOutputStream(errorFile))) {
            String date = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss"));
            stream.writeBytes((
                    "Identifier: " + identifier + "\n" +
                            "Time: " + date + "\n"
            ).getBytes(StandardCharsets.UTF_8));

            for (Map.Entry<String, String> entry : additional.entrySet())
                stream.writeBytes((entry.getKey() + ": " + entry.getValue() + "\n").getBytes(StandardCharsets.UTF_8));

            stream.writeBytes((
                    "\n" +
                            "-".repeat(60) + "[ Stacktrace ]" + "-".repeat(60) + "\n" +
                            "\n"
            ).getBytes(StandardCharsets.UTF_8));

            stream.flush();

            throwable.printStackTrace(stream);
        } catch (FileNotFoundException e) {
            CraftsNet.instance().logger().error(e, "Failed to create error log file");
        }

        return identifier;
    }

    /**
     * Ensures that the parent folder of the given file exists.
     * If the parent folder of the file does not exist, this method creates the necessary folder hierarchy.
     *
     * @param file The file whose parent folder needs to be ensured.
     */
    private static void ensureParentFolder(File file) {
        if (file.getParentFile() != null && !file.getParentFile().exists()) file.getParentFile().mkdirs();
    }

    /**
     * A subclass of {@link PrintStream} used for logging.
     * This class intercepts the output written to the stream and writes it to the provided output stream while removing any ASCII colors.
     */
    private static class LoggerPrintStream extends PrintStream {

        // Regular expression pattern to match ANSI escape sequences for colors
        private static final Pattern pattern = Pattern.compile("\u001B\\[[;\\d]*m");

        // The output stream where the log messages will be written
        private final OutputStream stream;

        /**
         * Constructs a new {@code LoggerPrintStream} instance.
         *
         * @param console The original console print stream.
         * @param stream  The output stream where the log messages will be written.
         */
        public LoggerPrintStream(PrintStream console, OutputStream stream) {
            super(console);
            this.stream = stream;
        }

        /**
         * Writes the specified string to the output stream after removing ASCII colors.
         *
         * @param s The string to be written.
         */
        @Override
        public void print(@Nullable String s) {
            super.print(s);
            try {
                stream.write(removeAsciiColors(s).getBytes(StandardCharsets.UTF_8));
                stream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Writes the specified string followed by a line separator to the output stream.
         *
         * @param x The string to be written.
         */
        @Override
        public void println(@Nullable String x) {
            if (x != null && x.contains("SLF4J")) return;
            super.println(x);
            try {
                stream.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                stream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Closes the output stream.
         */
        @Override
        public void close() {
            super.close();
            try {
                stream.flush();
                stream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Removes ASCII color codes from the input string.
         *
         * @param input The input string possibly containing ASCII color codes.
         * @return The input string with ASCII color codes removed.
         */
        protected static String removeAsciiColors(String input) {
            return input == null ? "null" : pattern.matcher(input).replaceAll("");
        }

    }

}
