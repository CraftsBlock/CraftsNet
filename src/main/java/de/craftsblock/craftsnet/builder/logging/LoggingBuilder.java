package de.craftsblock.craftsnet.builder.logging;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;

/**
 * Builder for configuring the logging system.
 * <p>
 * This class extends {@link NamespacedBuilder} and provides configuration options for
 * the logger implementation, log rotation, file logging, and IP address visibility.
 * <p>
 * The builder uses the {@code logging} namespace and registers itself with the root
 * {@link CraftsNetBuilder}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see NamespacedBuilder
 * @see Logger
 * @see ArgumentTransformer
 * @since 4.0.0
 */
public class LoggingBuilder extends NamespacedBuilder<LoggingBuilder> {

    /**
     * Creates and initializes the logging builder.
     * <p>
     * The constructor assigns the {@code logging} namespace, registers this builder
     * with the root builder, registers the supported logging arguments, and applies
     * the default configuration.
     *
     * @param root the root CraftsNet builder this logging builder belongs to
     */
    public LoggingBuilder(@NotNull CraftsNetBuilder root) {
        super(root, "logging");
        this.acquire(this);

        this.addKnownArgument("logger", ArgumentTransformer.classInstance(Logger.class));
        this.addKnownArgument("rotate", ArgumentTransformer.integerNumber(-1, Integer.MAX_VALUE));
        this.addKnownArgument("file", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("hideIps", ArgumentTransformer.booleanFlag());

        this.rotate(-1);
        this.file(true);
        this.hideIps(false);
    }

    /**
     * Sets the logger implementation to use.
     *
     * @param logger the logger instance to configure
     * @return this builder instance for method chaining
     * @throws NullPointerException if {@code logger} is {@code null}
     */
    public LoggingBuilder logger(@NotNull Logger logger) {
        return this.set("logger", logger);
    }

    /**
     * Retrieves the configured logger instance.
     *
     * @return the configured logger, or {@code null} if no logger has been configured
     * @see Logger
     */
    public Logger logger() {
        return this.getTyped("logger", Logger.class);
    }

    /**
     * Sets the log rotation threshold.
     * <p>
     * A value of {@code -1} can be used to represent the special default or disabled
     * rotation setting, depending on how the logging implementation interprets it.
     *
     * @param rotate the configured rotation value, between {@code -1} and {@link Integer#MAX_VALUE}
     * @return this builder instance for method chaining
     */
    public LoggingBuilder rotate(@Range(from = -1, to = Integer.MAX_VALUE) int rotate) {
        return this.set("rotate", rotate);
    }

    /**
     * Retrieves the configured log rotation value.
     *
     * @return the configured rotation value
     * @throws NullPointerException if no rotation value is configured
     */
    public int rotate() {
        return Objects.requireNonNull(
                this.getInt("rotate"),
                "No rotate amount found for logging"
        );
    }

    /**
     * Enables or disables logging to a file.
     *
     * @param file {@code true} to enable file logging; {@code false} to disable it
     * @return this builder instance for method chaining
     */
    public LoggingBuilder file(boolean file) {
        return this.set("file", file);
    }

    /**
     * Retrieves whether file logging is enabled.
     *
     * @return {@code true} if file logging is enabled; otherwise {@code false}
     * @throws NullPointerException if no file logging flag is configured
     */
    public boolean file() {
        return Objects.requireNonNull(
                this.getBoolean("file"),
                "No file flag found for logging"
        );
    }

    /**
     * Enables or disables the hiding of IP addresses in log output.
     *
     * @param hideIps {@code true} to hide IP addresses; {@code false} to leave them visible
     * @return this builder instance for method chaining
     */
    public LoggingBuilder hideIps(boolean hideIps) {
        return this.set("hideIps", hideIps);
    }

    /**
     * Retrieves whether IP addresses should be hidden in log output.
     *
     * @return {@code true} if IP addresses should be hidden; otherwise {@code false}
     * @throws NullPointerException if no IP-hiding flag is configured
     */
    public boolean hideIps() {
        return Objects.requireNonNull(
                this.getBoolean("hideIps"),
                "No hide ips flag found for logging"
        );
    }

}
