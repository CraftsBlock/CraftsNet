package de.craftsblock.craftsnet.builder.logging;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;

public class LoggingBuilder extends NamespacedBuilder<LoggingBuilder> {

    public LoggingBuilder(@NotNull CraftsNetBuilder root, int rotate, boolean file, boolean hideIps) {
        super(root, "log");
        this.acquire(this);

        this.addKnownArgument("logger", ArgumentTransformer.classInstance(Logger.class));
        this.addKnownArgument("rotate", ArgumentTransformer.number(-1, Integer.MAX_VALUE));
        this.addKnownArgument("file", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("hideIps", ArgumentTransformer.booleanFlag());

        this.rotate(rotate);
        this.file(file);
        this.hideIps(hideIps);
    }

    public LoggingBuilder logger(@NotNull Logger logger) {
        return this.set("logger", logger);
    }

    public Logger logger() {
        return this.getTyped("logger", Logger.class);
    }

    public LoggingBuilder rotate(@Range(from = -1, to = Integer.MAX_VALUE) int rotate) {
        return this.set("rotate", rotate);
    }

    public int rotate() {
        return Objects.requireNonNull(
                this.getInt("rotate"),
                "No rotate amount found for logging"
        );
    }

    public LoggingBuilder file(boolean file) {
        return this.set("file", file);
    }

    public boolean file() {
        return Objects.requireNonNull(
                this.getBoolean("file"),
                "No file flag found for logging"
        );
    }

    public LoggingBuilder hideIps(boolean hideIps) {
        return this.set("hideIps", hideIps);
    }

    public boolean hideIps() {
        return Objects.requireNonNull(
                this.getBoolean("hideIps"),
                "No hide ips flag found for logging"
        );
    }

}
