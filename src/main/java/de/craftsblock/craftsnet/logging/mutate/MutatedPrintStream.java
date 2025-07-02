package de.craftsblock.craftsnet.logging.mutate;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * A subclass of {@link PrintStream} used for logging.
 * This class intercepts the output written to the stream and writes it to the provided output stream while removing any ASCII colors.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.0
 * @since 3.0.2-SNAPSHOT
 */
class MutatedPrintStream extends PrintStream {

    // Regular expression pattern to match ANSI escape sequences for colors
    private static final Pattern pattern = Pattern.compile("\u001B\\[[;\\d]*m");

    private final LogStream logStream;
    private final OutputStream stream;

    private int skipLines = 0;

    /**
     * Constructs a new {@link MutatedPrintStream} instance.
     *
     * @param console The original console print stream.
     * @param stream  The output stream where the log messages will be written.
     */
    MutatedPrintStream(LogStream logStream, PrintStream console, @Nullable OutputStream stream) {
        super(console);
        this.logStream = logStream;
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

        String mutated = this.mutateLogLine(s);
        super.print(mutated);

        if (stream == null) return;
        try {
            stream.write(removeAsciiColors(mutated).getBytes(StandardCharsets.UTF_8));
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
        if (x == null) return;
        super.println(x);

        if (stream == null) return;
        try {
            stream.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
            stream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                mutator -> line.set(mutator.mutate(logStream, line.get()))
        );

        return line.get();
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
