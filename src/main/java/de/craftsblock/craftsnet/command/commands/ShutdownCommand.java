package de.craftsblock.craftsnet.command.commands;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.command.Command;
import de.craftsblock.craftsnet.command.CommandExecutor;
import de.craftsblock.craftsnet.utils.Logger;
import de.craftsblock.craftsnet.utils.Utils;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code ShutdownCommand} class implements the {@code CommandExecutor} interface
 * and defines the behavior of the "shutdown" command.
 * This command is used to gracefully shut down the CraftsNet program.
 *
 * @author CraftsBlock
 * @since 2.2.0
 */
public class ShutdownCommand implements CommandExecutor {

    /**
     * Executes the "shutdown" command, initiating the shutdown process for the CraftsNet program.
     *
     * @param command The {@code Command} object representing the "shutdown" command.
     * @param args    The arguments passed with the command (not used in this command).
     * @param logger  The logger for outputting messages or logging.
     */
    @Override
    public void onCommand(@NotNull Command command, @NotNull String[] args, @NotNull Logger logger) {
        logger.info("Shutdown anfrage wurde gesendet!");
        CraftsNet.addonManager.stop(); // Stop any registered addons
        if (CraftsNet.webServer != null) CraftsNet.webServer.stop(); // Stop the web server if it exists
        if (CraftsNet.webSocketServer != null)
            CraftsNet.webSocketServer.stop(); // Stop the WebSocket server if it exists
        // Interrupt the console reader thread if it exists
        Thread thread = Utils.getThreadByName("Console Reader");
        if (thread != null) thread.interrupt();
        logger.info("Programm erfolgreich beendet");
        System.exit(0); // Terminate the program with exit code 0 (success)
    }

}
