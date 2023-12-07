package de.craftsblock.craftsnet.command.commands;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.AddonManager;
import de.craftsblock.craftsnet.command.Command;
import de.craftsblock.craftsnet.command.CommandExecutor;
import de.craftsblock.craftsnet.utils.Logger;
import org.jetbrains.annotations.NotNull;

public class PluginCommand implements CommandExecutor {

    @Override
    public void onCommand(@NotNull Command command, @NotNull String[] args, @NotNull Logger logger) {
        AddonManager addonManager = CraftsNet.addonManager;
        if (CraftsNet.addonManager.getAddons().isEmpty()) {
            logger.info("Currently there are no addons installed!");
            return;
        }
        logger.info(
                "Plugins (" + addonManager.getAddons().size() + "): " +
                        String.join(", ", addonManager.getAddons().values().stream().map(Addon::getName).toList())
        );
    }

}
