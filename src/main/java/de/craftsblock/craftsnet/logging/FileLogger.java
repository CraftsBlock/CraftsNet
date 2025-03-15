package de.craftsblock.craftsnet.logging;

import de.craftsblock.craftscore.utils.FileUtils;
import de.craftsblock.craftscore.utils.id.Snowflake;
import de.craftsblock.craftsnet.CraftsNet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A utility class for logging to files.
 * This class provides methods to log messages to files, handle error logs, and redirect standard output and error streams to log files.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.3
 * @since 3.0.2-SNAPSHOT
 */
public class FileLogger {

    private final Path folder = Path.of("logs");
    private final long max;

    private OutputStream stream;
    private PrintStream oldOut;
    private PrintStream oldErr;

    /**
     * Constructs a new {@link FileLogger} instance with a specified maximum number of log files.
     *
     * @param max the maximum number of log files to retain. Once this limit is reached, older log files
     *            may be deleted or rotated out to maintain the limit.
     */
    public FileLogger(long max) {
        this.max = max;
    }

    /**
     * Starts logging to files.
     * This method sets up file logging by redirecting standard output and error streams to log files.
     */
    public void start() {
        if (stream != null) return;

        try {
            oldOut = System.out;
            oldErr = System.err;

            if (max != 0 && Files.exists(folder))
                try (Stream<Path> stream = Files.walk(folder, 1).parallel().filter(Files::isRegularFile)) {
                    long count = stream.count();
                    if (count >= max) {
                        long diff = count - (max - 1);
                        for (Path path : FileUtils.getOldestNFiles(folder, diff))
                            Files.deleteIfExists(path);
                    }
                }

            Path file = folder.resolve("latest.log");
            if (Files.exists(folder.resolve("latest.log"))) {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                OffsetDateTime creationTime = OffsetDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
                String prefix = creationTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                int i = 1;
                while (true) {
                    Path newName = folder.resolve(prefix + "_" + i + ".log");
                    if (Files.exists(newName)) {
                        i++;
                        continue;
                    }
                    Files.move(file, newName);
                    start();
                    return;
                }
            }

            ensureParentFolder(file);
            Files.createFile(file);

            stream = Files.newOutputStream(file);
            System.setOut(new LoggerPrintStream(oldOut, stream));
            System.setErr(new LoggerPrintStream(oldErr, stream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stops the logging to files
     */
    public void stop() {
        OutputStream out = System.out;
        OutputStream err = System.err;

        if (oldOut != null) System.setOut(oldOut);
        if (oldErr != null) System.setErr(oldErr);

        try {
            out.close();
            err.close();
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
    public synchronized void addLine(String line) {
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
     * @param craftsNet The CraftsNet instance which instantiates this
     * @param throwable The throwable for which to create the error log file.
     * @param protocol  The protocol associated with the error.
     * @param path      The path associated with the error.
     * @return The identifier of the error log file.
     */
    public long createErrorLog(CraftsNet craftsNet, Throwable throwable, String protocol, String path) {
        return createErrorLog(craftsNet, throwable, Map.of("Protocol", protocol, "Path", path));
    }

    /**
     * Creates an error log file for the given throwable.
     * This method creates an error log file containing the stack trace of the given throwable.
     *
     * @param craftsNet The CraftsNet instance which instantiates this
     * @param throwable The throwable for which to create the error log file.
     * @return The identifier of the error log file.
     */
    public long createErrorLog(CraftsNet craftsNet, Throwable throwable) {
        return createErrorLog(craftsNet, throwable, Collections.emptyMap());
    }

    /**
     * Creates an error log file for the given throwable.
     * This method creates an error log file containing the stack trace of the given throwable, along with additional information.
     *
     * @param craftsNet  The CraftsNet instance which instantiates this file logger.
     * @param throwable  The throwable for which to create the error log file.
     * @param additional Additional information to include in the error log file.
     * @return The identifier of the error log file.
     */
    public synchronized long createErrorLog(CraftsNet craftsNet, Throwable throwable, Map<String, String> additional) {
        try {
            Path errors = folder.resolve("errors");
            ensureParentFolder(errors);
            if (Files.notExists(errors))
                Files.createDirectory(errors);

            long identifier = Snowflake.generate();
            Path errorFile = errors.resolve("error_" + identifier + ".log");
            try (PrintStream stream = new PrintStream(Files.newOutputStream(errorFile))) {
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
                craftsNet.logger().error(e, "Failed to create error log file");
            }

            return identifier;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures that the parent directory of the specified file path exists.
     *
     * @param file the {@link Path} of the file for which the parent directory should be verified.
     */
    private void ensureParentFolder(Path file) {
        try {
            if (file.getParent() != null && Files.notExists(file.getParent())) Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Skips the next input on the {@link System#out} stream, if it is a file log stream.
     */
    public synchronized void skipNext() {
        this.skipNext(1);
    }


    /**
     * Skips the next n inputs on the {@link System#out} stream, if it is a file log stream.
     *
     * @param line the amount of inputs to be skipped
     */
    public synchronized void skipNext(@Range(from = 1, to = Integer.MAX_VALUE) int line) {
        if (System.out instanceof LoggerPrintStream logStream)
            logStream.skipNext(line);
    }

    /**
     * A subclass of {@link PrintStream} used for logging.
     * This class intercepts the output written to the stream and writes it to the provided output stream while removing any ASCII colors.
     */
    private static class LoggerPrintStream extends PrintStream {

        // Regular expression pattern to match ANSI escape sequences for colors
        private static final Pattern pattern = Pattern.compile("\u001B\\[[;\\d]*m");

        private final OutputStream stream;

        private int skipLines = 0;

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
            if (skipLines >= 1) {
                skipLines--;
                return;
            }

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

        /**
         * Skips the next input.
         */
        public synchronized void skipNext() {
            this.skipNext(1);
        }


        /**
         * Skips the next n inputs.
         *
         * @param line the amount of inputs to be skipped
         */
        public synchronized void skipNext(@Range(from = 1, to = Integer.MAX_VALUE) int line) {
            this.skipLines += line;
        }

    }

}
