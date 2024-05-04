package de.craftsblock.craftsnet.listeners;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.events.ConsoleMessageEvent;
import de.craftsblock.craftsnet.logging.FileLogger;

import java.util.Arrays;

import static de.craftsblock.craftscore.event.EventPriority.MONITOR;

/**
 * The {@code ConsoleListener} class is responsible for handling console messages and executing commands.
 * It listens for the {@code ConsoleMessageEvent} and processes console input into commands.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see de.craftsblock.craftsnet.command.CommandRegistry
 * @since CraftsNet-2.2.0
 */
public class ConsoleListener implements ListenerAdapter {

    private final CraftsNet craftsNet;

    /**
     * Constructs a new instance of a console listener
     *
     * @param craftsNet The CraftsNet instance which instantiates this console listener
     */
    public ConsoleListener(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Event handler for console messages.
     *
     * @param event The {@link ConsoleMessageEvent} which has been fired.
     */
    @EventHandler(priority = MONITOR)
    public void handleConsoleMessage(ConsoleMessageEvent event) {
        if (event.isCancelled())
            return;
        String message = event.getMessage();
        if (craftsNet.fileLogger() != null) craftsNet.fileLogger().addLine("> " + message);
        String[] args = message.split(" ");
        String command = args[0];
        args = Arrays.stream(args).skip(1).toArray(String[]::new);
        craftsNet.commandRegistry().perform(command, args);
    }

}
