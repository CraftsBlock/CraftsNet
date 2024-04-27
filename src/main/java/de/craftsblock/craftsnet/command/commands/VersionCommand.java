package de.craftsblock.craftsnet.command.commands;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.command.Command;
import de.craftsblock.craftsnet.command.CommandExecutor;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Command executor for the version command.
 * This class implements the {@link CommandExecutor} interface and provides functionality to handle the "version" command.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see CommandExecutor
 * @since CraftsNet-3.0.0
 */
public class VersionCommand implements CommandExecutor {

    /**
     * Executes the version command.
     * This method logs the current version of CraftsNet to the provided logger.
     *
     * @param command The command that was executed.
     * @param args    The arguments passed with the command (not used in this implementation).
     * @param logger  The logger used to log messages.
     */
    @Override
    public void onCommand(@NotNull Command command, @NotNull String[] args, @NotNull Logger logger) {
        logger.info("You are using CraftsNet v" + CraftsNet.version);
    }

}
