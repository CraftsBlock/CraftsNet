package de.craftsblock.craftsnet.command;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.command.commands.PluginCommand;
import de.craftsblock.craftsnet.command.commands.ReloadCommand;
import de.craftsblock.craftsnet.command.commands.ShutdownCommand;
import de.craftsblock.craftsnet.command.commands.VersionCommand;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code CommandRegistry} class manages and provides access to registered commands.
 * It allows commands to be retrieved, checked for existence, and executed.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @see Command
 * @since 2.2.0-SNAPSHOT
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

        craftsNet.logger().debug("Registering the default commands");
        getCommand("pl").setExecutor(new PluginCommand(craftsNet));
        getCommand("pl").addAlias("plugin", "plugins", "addons");
        getCommand("restart").setExecutor(new ReloadCommand(craftsNet));
        getCommand("restart").addAlias("reload", "rl");
        getCommand("shutdown").setExecutor(new ShutdownCommand(craftsNet));
        getCommand("shutdown").addAlias("quit", "exit", "stop");
        getCommand("ver").setExecutor(new VersionCommand());
        getCommand("ver").addAlias("version", "v");
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
        Command command;

        if (commands.containsKey(name)) command = commands.get(name);
        else command = commands.values().stream()
                .filter(cmd -> cmd.isAlias(name))
                .findFirst()
                .orElse(null);

        if (command == null) {
            logger.warning("This command was not found!");
            return;
        }

        command.getExecutor().onCommand(command, name, args, logger);
    }

}
