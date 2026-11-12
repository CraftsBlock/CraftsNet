package de.craftsblock.craftsnet.logging.mutate;

import de.craftsblock.craftscore.utils.FileUtils;
import de.craftsblock.craftscore.utils.id.Snowflake;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.logging.mutate.builtin.BlurIPsMutator;
import de.craftsblock.craftsnet.logging.mutate.stream.LogOutputStream;
import de.craftsblock.craftsnet.logging.mutate.stream.MutatedPrintStream;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * A utility class for logging to files.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see MutatedPrintStream
 * @since 3.0.2-SNAPSHOT
 */
public class LogStream {

    private static final Path LOG_FOLDER = Path.of("logs");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static final String ERROR_SEPARATOR =
            "-".repeat(60) + "[ Stacktrace ]" + "-".repeat(60);

    private final CraftsNet craftsNet;
    private final boolean logToFiles;
    private final long maxLogFiles;
    private final long maxErrorFiles;

    private final CopyOnWriteArrayList<LogStreamMutator> logStreamMutators =
            new CopyOnWriteArrayList<>();

    private final List<LogStreamMutator> unmodifiableLogStreamMutators =
            Collections.unmodifiableList(logStreamMutators);

    private @Nullable OutputStream stream;

    private @Nullable PrintStream oldOut;
    private @Nullable PrintStream oldErr;

    private @Nullable MutatedPrintStream installedOut;
    private @Nullable MutatedPrintStream installedErr;

    /**
     * Constructs a new LogStream.
     *
     * @param craftsNet     The owning CraftsNet instance.
     * @param logToFiles    Whether file logging is enabled.
     * @param maxLogFiles   Maximum number of regular log files.
     *                      Zero disables rotation.
     * @param maxErrorFiles Maximum number of error log files.
     *                      Zero disables rotation.
     */
    public LogStream(
            CraftsNet craftsNet,
            boolean logToFiles,
            long maxLogFiles,
            long maxErrorFiles
    ) {
        this.craftsNet = Objects.requireNonNull(craftsNet, "craftsNet");
        this.logToFiles = logToFiles;

        if (maxLogFiles < 0 || maxErrorFiles < 0) {
            throw new IllegalArgumentException(
                    "Log file limits must not be negative."
            );
        }

        this.maxLogFiles = maxLogFiles;
        this.maxErrorFiles = maxErrorFiles;

        registerLogStreamMutator(new BlurIPsMutator());
    }

    /**
     * Registers a log stream mutator.
     *
     * @param mutator The mutator to register.
     */
    public void registerLogStreamMutator(LogStreamMutator mutator) {
        logStreamMutators.add(Objects.requireNonNull(mutator, "mutator"));
    }

    /**
     * Unregisters a log stream mutator.
     *
     * @param mutator The mutator to unregister.
     */
    public void unregisterLogStreamMutator(LogStreamMutator mutator) {
        logStreamMutators.remove(mutator);
    }

    /**
     * Checks whether a mutator of the given class is registered.
     *
     * @param mutator The mutator to check.
     * @return Whether a mutator of the same class is registered.
     */
    public boolean isLogStreamMutatorRegistered(LogStreamMutator mutator) {
        Objects.requireNonNull(mutator, "mutator");

        Class<?> type = mutator.getClass();
        return logStreamMutators.stream()
                .anyMatch(registered -> registered.getClass().equals(type));
    }

    /**
     * Returns an unmodifiable, live view of registered mutators.
     *
     * @return The registered mutators.
     */
    public List<LogStreamMutator> getLogStreamMutators() {
        return unmodifiableLogStreamMutators;
    }

    /**
     * Starts file logging and redirects System.out and System.err.
     */
    public synchronized void start() {
        if (!logToFiles || stream != null) {
            return;
        }

        try {
            OutputStream fileStream = createFileLogStream();

            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            MutatedPrintStream redirectedOut = new MutatedPrintStream(
                    this,
                    new LogOutputStream(originalOut, fileStream)
            );

            MutatedPrintStream redirectedErr = new MutatedPrintStream(
                    this,
                    new LogOutputStream(originalErr, fileStream)
            );

            this.oldOut = originalOut;
            this.oldErr = originalErr;
            this.stream = fileStream;
            this.installedOut = redirectedOut;
            this.installedErr = redirectedErr;

            System.setOut(redirectedOut);
            System.setErr(redirectedErr);

        } catch (IOException e) {
            throw new RuntimeException("Failed to start file logging.", e);
        }
    }

    /**
     * Stops file logging and restores the original system streams.
     */
    public synchronized void stop() {
        MutatedPrintStream redirectedOut = installedOut;
        MutatedPrintStream redirectedErr = installedErr;

        try {
            if (redirectedOut != null) {
                redirectedOut.flush();
            }

            if (redirectedErr != null) {
                redirectedErr.flush();
            }

            if (redirectedOut != null && System.out == redirectedOut) {
                System.setOut(oldOut);
            }

            if (redirectedErr != null && System.err == redirectedErr) {
                System.setErr(oldErr);
            }

            if (stream != null) {
                stream.flush();
                stream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to stop file logging.", e);
        } finally {
            stream = null;
            oldOut = null;
            oldErr = null;
            installedOut = null;
            installedErr = null;
        }
    }

    /**
     * Adds a line directly to the current log file.
     *
     * @param line The line to write.
     */
    public synchronized void addLine(String line) {
        if (stream == null || line == null) {
            return;
        }

        try {
            String sanitized = LogOutputStream.removeAsciiColors(line);
            stream.write(
                    (sanitized + LINE_SEPARATOR).getBytes(StandardCharsets.UTF_8)
            );

            stream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write log line.", e);
        }
    }

    /**
     * Creates an error log containing the given exception.
     *
     * @param throwable The throwable to log.
     * @param protocol  The associated protocol.
     * @param path      The associated path.
     * @return The error log identifier.
     */
    public long createErrorLog(
            Throwable throwable,
            String protocol,
            String path
    ) {
        Map<String, String> additional = new LinkedHashMap<>();

        additional.put("Protocol", protocol);
        additional.put("Path", path);

        return createErrorLog(throwable, additional);
    }

    /**
     * Creates an error log containing the given exception.
     *
     * @param throwable The throwable to log.
     * @return The error log identifier.
     */
    public long createErrorLog(Throwable throwable) {
        return createErrorLog(throwable, Collections.emptyMap());
    }

    /**
     * Creates an error log containing the given exception and additional information.
     *
     * @param throwable  The throwable to log.
     * @param additional Additional information.
     * @return The error log identifier.
     */
    public synchronized long createErrorLog(
            Throwable throwable,
            Map<String, String> additional
    ) {
        Objects.requireNonNull(throwable, "throwable");
        Objects.requireNonNull(additional, "additional");

        try {
            Path errorsFolder = LOG_FOLDER.resolve("errors");

            Files.createDirectories(errorsFolder);
            rotateLogFiles(maxErrorFiles, errorsFolder);

            long identifier = Snowflake.generate();

            Path errorFile = errorsFolder.resolve(
                    "error_" + identifier + ".log"
            );

            try (PrintStream output = new PrintStream(
                    Files.newOutputStream(errorFile),
                    true,
                    StandardCharsets.UTF_8
            )) {
                output.println("Identifier: " + identifier);
                output.println(
                        "Time: " + OffsetDateTime.now().format(DATE_TIME_FORMAT)
                );

                for (Map.Entry<String, String> entry : additional.entrySet()) {
                    output.println(entry.getKey() + ": " + entry.getValue());
                }

                output.println();
                output.println(ERROR_SEPARATOR);
                output.println();

                throwable.printStackTrace(output);

                if (output.checkError()) {
                    throw new IOException(
                            "Failed to write error log: " + errorFile
                    );
                }
            }

            return identifier;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create error log.", e);
        }
    }

    /**
     * Skips the next line on System.out.
     */
    public synchronized void skipNext() {
        skipNext(1);
    }

    /**
     * Skips the next specified number of lines on System.out.
     *
     * @param line Number of lines to skip.
     */
    public synchronized void skipNext(
            @Range(from = 1, to = Integer.MAX_VALUE) int line
    ) {
        if (line < 1) {
            throw new IllegalArgumentException("line must be greater than zero.");
        }

        if (System.out instanceof MutatedPrintStream logStream) {
            logStream.skipNext(line);
        }
    }

    /**
     * Creates a new log file output stream.
     *
     * @return The file output stream.
     * @throws IOException If stream creation fails.
     */
    private OutputStream createFileLogStream() throws IOException {
        Files.createDirectories(LOG_FOLDER);

        rotateLogFiles(maxLogFiles, LOG_FOLDER);

        Path latestFile = LOG_FOLDER.resolve("latest.log");
        if (Files.exists(latestFile)) {
            rotateLatestLog(latestFile);
        }

        return Files.newOutputStream(latestFile);
    }

    /**
     * Rotates the existing latest.log file.
     *
     * @param latestFile The current latest log file.
     * @throws IOException If rotation fails.
     */
    private void rotateLatestLog(Path latestFile) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                latestFile,
                BasicFileAttributes.class
        );

        OffsetDateTime creationTime = OffsetDateTime.ofInstant(
                attributes.creationTime().toInstant(),
                ZoneId.systemDefault()
        );

        String prefix = creationTime.format(DATE_FORMAT);

        Path target;
        int index = 1;

        do {
            target = LOG_FOLDER.resolve(prefix + "_" + index++ + ".log");
        } while (Files.exists(target));

        try {
            Files.move(
                    latestFile,
                    target,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(latestFile, target);
        }
    }

    /**
     * Removes the oldest log files if the configured limit is exceeded.
     *
     * @param max    Maximum number of files. Zero disables rotation.
     * @param folder Folder containing the log files.
     */
    private void rotateLogFiles(long max, Path folder) {
        if (max == 0 || Files.notExists(folder)) {
            return;
        }

        try (Stream<Path> files = Files.walk(folder, 1)) {
            long count = files.filter(Files::isRegularFile).count();
            if (count < max) {
                return;
            }

            long amountToDelete = count - max + 1;
            for (Path file : FileUtils.getOldestNFiles(folder, amountToDelete)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to rotate log folder: " + folder,
                    e
            );
        }
    }

    /**
     * Returns the owning CraftsNet instance.
     *
     * @return The CraftsNet instance.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

}
