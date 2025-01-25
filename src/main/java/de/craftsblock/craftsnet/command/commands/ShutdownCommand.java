package de.craftsblock.craftsnet.command.commands;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.command.Command;
import de.craftsblock.craftsnet.command.CommandExecutor;
import de.craftsblock.craftsnet.logging.Logger;
import de.craftsblock.craftsnet.utils.Utils;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code ShutdownCommand} class implements the {@code CommandExecutor} interface
 * and defines the behavior of the "shutdown" command.
 * This command is used to gracefully shut down the CraftsNet program.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 2.2.0-SNAPSHOT
 */
public class ShutdownCommand implements CommandExecutor {

    private final CraftsNet craftsNet;

    /**
     * Constructs a new instance shutdown command.
     *
     * @param craftsNet The CraftsNet instance which instantiates this shutdown command
     */
    public ShutdownCommand(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Executes the "shutdown" command, initiating the shutdown process for CraftsNet.
     *
     * @param command The {@code Command} object representing the "shutdown" command.
     * @param alias   The alias name of the command used to access it.
     * @param args    The arguments passed with the command (not used in this command).
     * @param logger  The logger for outputting messages or logging.
     */
    @Override
    public void onCommand(@NotNull Command command, @NotNull String alias, @NotNull String[] args, @NotNull Logger logger) {
        logger.info("Shutdown anfrage wurde gesendet!");
        craftsNet.stop();
        // Interrupt the console reader thread if it exists
        Thread thread = Utils.getThreadByName("CraftsNet Console Reader");
        if (thread != null) thread.interrupt();
        logger.info("Programm erfolgreich beendet");
        System.exit(0); // Terminate the program with exit code 0 (success)
    }

}
