package de.craftsblock.craftsnet.command;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The {@code Command} class represents a command that can be executed.
 * It contains information about the command's name and its associated executor.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1
 * @see CommandExecutor
 * @see CommandRegistry
 * @since CraftsNet-2.2.0
 */
public class Command {

    private final String name;
    private final ConcurrentLinkedQueue<String> aliases = new ConcurrentLinkedQueue<>();
    private CommandExecutor executor;

    /**
     * Constructs a new {@code Command} with the specified name.
     *
     * @param name The name of the command. It uniquely identifies the command.
     */
    public Command(String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the command.
     *
     * @return The name of the command, which is a unique identifier.
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if the command has an associated executor.
     *
     * @return {@code true} if an executor is set for this command, otherwise {@code false}.
     */
    public boolean hasExecutor() {
        return executor != null;
    }

    /**
     * Sets the executor for this command. The executor is responsible for handling the command's logic.
     *
     * @param executor The executor that will handle the execution of this command.
     */
    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    /**
     * Retrieves the executor associated with this command.
     *
     * @return The executor for this command, or {@code null} if no executor is set.
     */
    public CommandExecutor getExecutor() {
        return executor;
    }

    /**
     * Adds an alias to this command
     *
     * @param alias The alias which should be added
     */
    public void addAlias(String... alias) {
        aliases.addAll(Arrays.asList(alias));
    }

    /**
     * Checks if an alias is already present.
     *
     * @param alias The name of the alias which should be checked.
     * @return true if the alias is already present, false otherwise.
     */
    public boolean isAlias(String alias) {
        return aliases.contains(alias);
    }

}
