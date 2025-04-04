package de.craftsblock.craftsnet.command;

import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code CommandExecutor} interface represents an object capable of executing commands.
 * Classes that implement this interface can define custom logic for handling commands.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see Command
 * @since 2.2.0-SNAPSHOT
 */
public interface CommandExecutor {

    /**
     * Executes a command with the provided arguments and logger.
     *
     * @param command The command to be executed.
     * @param alias   The alias name of the command used to access it.
     * @param args    The arguments passed with the command.
     * @param logger  The logger for outputting messages or logging.
     */
    void onCommand(@NotNull Command command, String alias, @NotNull String[] args, @NotNull Logger logger);

}
