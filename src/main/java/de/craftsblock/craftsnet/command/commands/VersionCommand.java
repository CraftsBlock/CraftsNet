package de.craftsblock.craftsnet.command.commands;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.command.Command;
import de.craftsblock.craftsnet.command.CommandExecutor;
import de.craftsblock.craftsnet.utils.Logger;
import org.jetbrains.annotations.NotNull;

public class VersionCommand implements CommandExecutor {

    @Override
    public void onCommand(@NotNull Command command, @NotNull String[] args, @NotNull Logger logger) {
        logger.info("You are using CraftsNet v" + CraftsNet.version);
    }

}
