package de.craftsblock.craftsnet.logging.mutate;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for mutating log output lines before they are written to the output stream.
 * <p>
 * Implementations of this interface can be used to modify, format, or filter log lines dynamically
 * based on the provided {@link LogStream} context.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see LogStream
 * @since 3.4.4
 */
@FunctionalInterface
public interface LogStreamMutator {

    /**
     * Mutates the given log line based on the state or configuration of the associated {@link LogStream}.
     *
     * @param logStream The {@link LogStream} that produced the original log line.
     * @param line      The original log line.
     * @return The mutated (possibly modified or filtered) log line. Must not be {@code null}.
     */
    @NotNull String mutate(@NotNull LogStream logStream, @NotNull String line);

}
