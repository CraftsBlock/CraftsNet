package de.craftsblock.craftsnet.logging.mutate.builtin;

import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.logging.mutate.LogStream;
import de.craftsblock.craftsnet.logging.mutate.LogStreamMutator;
import de.craftsblock.craftsnet.utils.Utils;
import org.jetbrains.annotations.NotNull;

/**
 * A built-in {@link LogStreamMutator} that blurs or censors IP addresses in log lines.
 * <p>
 * This mutator checks the {@link CraftsNetBuilder#shouldHideIps()} setting in the
 * {@link de.craftsblock.craftsnet.CraftsNet} builder and, if enabled, replaces all
 * IP addresses in the output with a masked version.
 * <p>
 * Useful for privacy-sensitive environments such as public log files or shared infrastructure.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Utils#blurIPs(String)
 * @since 3.5.0
 */
public class BlurIPsMutator implements LogStreamMutator {

    /**
     * Mutates the log line blurring the line if it is enabled.
     *
     * @param logStream The {@link LogStream} that produced the original log line.
     * @param line      The original log line.
     * @return The mutated log line.
     */
    @Override
    public @NotNull String mutate(@NotNull LogStream logStream, @NotNull String line) {
        if (!logStream.getCraftsNet().getBuilder().shouldHideIps()) return line;
        return Utils.blurIPs(line);
    }

}
