package de.craftsblock.craftsnet.command.commands;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.command.Command;
import de.craftsblock.craftsnet.command.CommandExecutor;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Command executor for the reload command.
 * This class implements the {@link CommandExecutor} interface and provides functionality to handle the "reload" command.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.5-SNAPSHOT
 */
public class ReloadCommand implements CommandExecutor {

    private final CraftsNet craftsNet;

    /**
     * Constructs a new instance of the reload command.
     *
     * @param craftsNet The CraftsNet instance for which the reload command was registered
     */
    public ReloadCommand(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Executes the reload command.
     *
     * @param command The command to be executed.
     * @param alias   The alias name of the command used to access it.
     * @param args    The arguments passed with the command.
     * @param logger  The logger for outputting messages or logging.
     */
    @Override
    public void onCommand(@NotNull Command command, @NotNull String alias, @NotNull String[] args, @NotNull Logger logger) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("confirm")) {
            logger.warning("Please use '" + alias + " confirm' if you really want to reload CraftsNet as it may break some things.");
            return;
        }

        try {
            logger.info("Restarting CraftsNet...");
            craftsNet.restart(() -> {
                System.out.println();
                System.out.println();
            });
        } catch (Exception e) {
            logger.error(e, "Could not restart CraftsNet");
        }
    }

}
