package de.craftsblock.craftsnet.command.commands;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.command.Command;
import de.craftsblock.craftsnet.command.CommandExecutor;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Command executor for the plugin command.
 * This class implements the {@link CommandExecutor} interface and provides functionality to handle the "plugin" command.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-3.0.2
 */
public class PluginCommand implements CommandExecutor {

    private final CraftsNet craftsNet;

    /**
     * Constructs a new instance of the plugin command
     *
     * @param craftsNet The CraftsNet instance which instantiates this plugin command.
     */
    public PluginCommand(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Executes the plugin command.
     * This method retrieves the {@link AddonManager} from CraftsNet and logs information about installed addons to the provided logger.
     *
     * @param command The command that was executed.
     * @param args    The arguments passed with the command (not used in this implementation).
     * @param logger  The logger used to log messages.
     */
    @Override
    public void onCommand(@NotNull Command command, @NotNull String[] args, @NotNull Logger logger) {
        AddonManager addonManager = craftsNet.addonManager();
        if (addonManager.getAddons().isEmpty()) {
            logger.info("Currently there are no addons installed!");
            return;
        }
        logger.info(
                "Plugins (" + addonManager.getAddons().size() + "): " +
                        String.join(", ", addonManager.getAddons().values().stream().map(Addon::getName).toList())
        );
    }

}
