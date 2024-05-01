package de.craftsblock.craftsnet.command;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code CommandRegistry} class manages and provides access to registered commands.
 * It allows commands to be retrieved, checked for existence, and executed.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0
 * @see Command
 * @since CraftsNet-2.2.0
 */
public class CommandRegistry {

    private final ConcurrentHashMap<String, Command> commands = new ConcurrentHashMap<>();
    private final CraftsNet craftsNet;

    /**
     * Constructs a new instance of the command registry.
     *
     * @param craftsNet The CraftsNet instance which instantiates this command registry.
     */
    public CommandRegistry(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Retrieves a command by name. If the command doesn't exist, it is created.
     *
     * @param name The name of the command to retrieve.
     * @return The command with the specified name or a new command if it doesn't exist.
     */
    @NotNull
    public Command getCommand(String name) {
        return commands.computeIfAbsent(name, Command::new);
    }

    /**
     * Checks if a command with the given name exists.
     *
     * @param name The name of the command to check for existence.
     * @return {@code true} if a command with the specified name exists, otherwise {@code false}.
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }

    /**
     * Performs the execution of a command with the provided name and arguments.
     *
     * @param name The name of the command to execute.
     * @param args The arguments to pass to the command executor.
     */
    public void perform(String name, String[] args) {
        Logger logger = craftsNet.logger();
        Command command = null;
        for (Command tmp : commands.values())
            if (tmp.getName().equalsIgnoreCase(name) || tmp.isAlias(name)) {
                command = tmp;
                break;
            }

        if (command == null) {
            logger.warning("This command was not found!");
            return;
        }

        command.getExecutor().onCommand(command, name, args, logger);
    }

}
