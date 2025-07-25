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
 * @author Philipp Maywald
 * @version 1.0.3
 * @see de.craftsblock.craftsnet.command.CommandRegistry
 * @since 2.2.0-SNAPSHOT
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
        if (event.isCancelled()) return;

        String message = event.getMessage();
        if (message.isBlank()) return;
        if (craftsNet.getLogStream() != null) craftsNet.getLogStream().addLine("> " + message);

        String[] cli = message.split(" ");

        String command = cli[0];
        String[] args = Arrays.stream(cli).skip(1).toArray(String[]::new);

        craftsNet.getCommandRegistry().perform(command, args);
    }

}
