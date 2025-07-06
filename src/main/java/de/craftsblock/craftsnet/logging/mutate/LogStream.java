package de.craftsblock.craftsnet.logging.mutate;

import de.craftsblock.craftscore.utils.FileUtils;
import de.craftsblock.craftscore.utils.id.Snowflake;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.logging.mutate.builtin.BlurIPsMutator;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * A utility class for logging to files.
 * This class provides methods to log messages to files, handle error logs, and redirect standard output and error streams to log files.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.1
 * @see MutatedPrintStream
 * @since 3.0.2-SNAPSHOT
 */
public class LogStream {

    private final Path folder = Path.of("logs");
    private final CraftsNet craftsNet;
    private final boolean logToFiles;
    private final long max;

    private final List<LogStreamMutator> logStreamMutators = new ArrayList<>();

    private OutputStream stream;
    private PrintStream oldOut;
    private PrintStream oldErr;

    /**
     * Constructs a new {@link LogStream} instance with a specified maximum number of log files.
     *
     * @param craftsNet  The instance of {@link CraftsNet}, which is using this logging utils.
     * @param logToFiles {@code true} if logging to files is enabled, {@code false} otherwise.
     * @param max        the maximum number of log files to retain. Once this limit is reached, older log files
     *                   may be deleted or rotated out to maintain the limit.
     */
    public LogStream(CraftsNet craftsNet, boolean logToFiles, long max) {
        this.craftsNet = craftsNet;
        this.logToFiles = logToFiles;
        this.max = max;

        this.registerLogStreamMutator(new BlurIPsMutator());
    }

    /**
     * Registers a new {@link LogStreamMutator} that will be applied to each line
     * written to the log stream.
     *
     * @param mutator The {@link LogStreamMutator} to register.
     * @since 3.5.0
     */
    public void registerLogStreamMutator(LogStreamMutator mutator) {
        synchronized (logStreamMutators) {
            logStreamMutators.add(mutator);
        }
    }

    /**
     * Unregisters a previously registered {@link LogStreamMutator}.
     *
     * @param mutator The {@link LogStreamMutator} to remove.
     * @since 3.5.0
     */
    public void unregisterLogStreamMutator(LogStreamMutator mutator) {
        synchronized (logStreamMutators) {
            logStreamMutators.remove(mutator);
        }
    }

    /**
     * Returns an unmodifiable list of all currently registered {@link LogStreamMutator}s.
     *
     * @return A list of registered log stream mutators.
     * @since 3.5.0
     */
    public List<LogStreamMutator> getLogStreamMutators() {
        synchronized (logStreamMutators) {
            return Collections.unmodifiableList(logStreamMutators);
        }
    }

    /**
     * Starts logging to files.
     * This method sets up file logging by redirecting standard output and error streams to log files.
     */
    public void start() {
        if (System.out instanceof MutatedPrintStream || System.err instanceof MutatedPrintStream) return;

        try {
            oldOut = System.out;
            oldErr = System.err;

            this.stream = this.createFileLogStream();
            System.setOut(new MutatedPrintStream(this, oldOut, stream));
            System.setErr(new MutatedPrintStream(this, oldErr, stream));
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

            if (stream == null) return;
            stream.flush();
            stream.close();
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
        if (stream == null || line == null) return;

        try {
            stream.write((MutatedPrintStream.removeAsciiColors(line) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
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
                craftsNet.getLogger().error(e, "Failed to create error log file");
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
        if (System.out instanceof MutatedPrintStream logStream)
            logStream.skipNext(line);
    }

    /**
     * Creates a log file output stream if file logging is activated respectively to {@link #logToFiles}.
     *
     * @return The {@link OutputStream} which log to a file, or null otherwise.
     * @throws IOException If the log file stream creation fails.
     * @since 3.4.0-SNAPSHOT
     */
    private @Nullable OutputStream createFileLogStream() throws IOException {
        if (!this.logToFiles) return null;

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
        while (!Thread.currentThread().isInterrupted()) {
            if (!Files.exists(file)) break;

            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            OffsetDateTime creationTime = OffsetDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
            String prefix = creationTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            int i = 1;
            AtomicReference<Path> name = new AtomicReference<>();
            while (!Thread.currentThread().isInterrupted()) {
                Path newName = folder.resolve(prefix + "_" + i + ".log");

                if (!Files.exists(newName)) {
                    name.set(newName);
                    break;
                }

                i++;
            }

            if (name.get() == null) return null;
            Files.move(file, name.get());
        }

        ensureParentFolder(file);
        Files.createFile(file);

        return Files.newOutputStream(file);
    }

    /**
     * The instance of {@link CraftsNet} which is owning the {@link LogStream}.
     *
     * @return The instance of {@link CraftsNet}.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

}
