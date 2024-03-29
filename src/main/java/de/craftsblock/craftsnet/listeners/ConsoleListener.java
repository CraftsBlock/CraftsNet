package de.craftsblock.craftsnet.listeners;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;

import java.util.Arrays;

import static de.craftsblock.craftscore.event.EventPriority.MONITOR;

/**
 * The {@code ConsoleListener} class is responsible for handling console messages and executing commands.
 * It listens for the {@code ConsoleMessageEvent} and processes console input into commands.
 *
 * @author CraftsBlock
 * @version 1.0
 * @see de.craftsblock.craftsnet.command.CommandRegistry
 * @since 2.2.0
 */
public class ConsoleListener implements ListenerAdapter {

    @EventHandler(priority = MONITOR)
    public void handleConsoleMessage(ConsoleMessageEvent event) {
        if (event.isCancelled())
            return;
        String[] args = event.getMessage().split(" ");
        String command = args[0];
        args = Arrays.stream(args).skip(1).toArray(String[]::new);
        CraftsNet.commandRegistry().perform(command, args);
    }

}
