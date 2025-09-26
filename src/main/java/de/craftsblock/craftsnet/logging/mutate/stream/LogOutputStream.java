package de.craftsblock.craftsnet.logging.mutate.stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

/**
 * An OutputStream that writes log data to a console PrintStream and optionally
 * to a file OutputStream. ASCII color codes (ANSI escape sequences) are removed
 * when writing to the file stream.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.5.4
 */
public class LogOutputStream extends FilterOutputStream {

    // Regular expression pattern to match ANSI escape sequences for colors
    private static final Pattern pattern = Pattern.compile("\u001B\\[[;\\d]*m");

    private final @Nullable OutputStream fileStream;

    /**
     * Constructs a new LogOutputStream that writes to the given console stream
     * and optionally to a file stream.
     *
     * @param consoleStream The PrintStream to write log messages to (console).
     * @param fileStream    The OutputStream to write log messages to (file), may be null.
     */
    public LogOutputStream(@NotNull PrintStream consoleStream, @Nullable OutputStream fileStream) {
        super(consoleStream);

        this.fileStream = fileStream;
    }

    /**
     * Writes a single byte to the console and file streams.
     *
     * @param b The byte to write.
     */
    @Override
    public synchronized void write(int b) {
        try {
            super.write(b);

            if (fileStream == null) return;
            fileStream.write(b);
        } catch (IOException e) {
            throw new RuntimeException("Could not log!", e);
        }
    }

    /**
     * Writes a portion of a byte array to the console and file streams.
     * ASCII color codes are removed when writing to the file stream.
     *
     * @param b   The byte array containing data to write.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public synchronized void write(byte @NotNull [] b, int off, int len) throws IOException {
        try {
            getConsoleStream().write(b, off, len);

            if (fileStream == null) return;

            String message = new String(b, off, len);
            fileStream.write(removeAsciiColors(message).getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Could not log!", e);
        }
    }

    /**
     * Flushes the console and file streams, ensuring all buffered output is written.
     */
    @Override
    public synchronized void flush() {
        try {
            super.flush();

            if (fileStream == null) return;
            fileStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Could not flush log stream!", e);
        }
    }

    /**
     * Flushes and closes the console and file streams.
     */
    @Override
    public synchronized void close() {
        try {
            this.flush();

            if (fileStream == null) return;
            fileStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not close log stream!", e);
        }
    }

    /**
     * Returns the console PrintStream.
     *
     * @return The console PrintStream used by this LogOutputStream.
     */
    public @NotNull PrintStream getConsoleStream() {
        return (PrintStream) out;
    }

    /**
     * Returns the file OutputStream, if present.
     *
     * @return The file OutputStream, or null if none was provided.
     */
    public @Nullable OutputStream getFileStream() {
        return fileStream;
    }

    /**
     * Removes ASCII color codes from the input string.
     *
     * @param input The input string possibly containing ASCII color codes.
     * @return The input string with ASCII color codes removed.
     */
    public static String removeAsciiColors(String input) {
        return input == null ? "null" : pattern.matcher(input).replaceAll("");
    }

}
