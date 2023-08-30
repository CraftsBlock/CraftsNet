package de.craftsblock.craftsnet.command;

/**
 * The {@code Command} class represents a command that can be executed.
 * It contains information about the command's name and its associated executor.
 *
 * @author CraftsBlock
 * @see CommandExecutor
 * @see CommandRegistry
 * @since 2.2.0
 */
public class Command {

    private final String name;
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

}
