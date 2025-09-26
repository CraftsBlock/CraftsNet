package de.craftsblock.craftsnet.logging.mutate.stream;

import de.craftsblock.craftsnet.logging.mutate.LogStream;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A subclass of {@link PrintStream} used for logging.
 * This class intercepts the output written to the stream and writes it to the provided output stream while removing any ASCII colors.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.3.0
 * @since 3.0.2-SNAPSHOT
 */
public class MutatedPrintStream extends PrintStream {

    private final LogStream logStream;

    private int skipLines = 0;

    /**
     * Constructs a new {@link MutatedPrintStream} instance.
     *
     * @param logStream   The parent {@link LogStream} instance.
     * @param destination The destination to which the print stream should print.
     */
    public MutatedPrintStream(LogStream logStream, OutputStream destination) {
        super(destination);
        this.logStream = logStream;
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

        super.print(mutateLogLine(s));
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() {
    }

    /**
     * Mutates the log lines. Blurs ips in the log line, if they
     * should be hidden in the console.
     *
     * @param input The log line.
     * @return The mutated log line.
     * @since 3.4.0-SNAPSHOT
     */
    protected String mutateLogLine(String input) {
        AtomicReference<String> line = new AtomicReference<>(input);

        logStream.getLogStreamMutators().forEach(
                mutator -> {
                    String mutated = mutator.mutate(logStream, line.get());

                    if (mutated == null) return;
                    line.set(mutated);
                }
        );

        return line.get();
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
