package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftscore.utils.Arguments;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.builder.addon.AddonSystemBuilder;
import de.craftsblock.craftsnet.builder.addon.AddonSystemState;
import de.craftsblock.craftsnet.builder.logging.LoggingBuilder;
import de.craftsblock.craftsnet.builder.server.ServerBuilder;
import de.craftsblock.craftsnet.builder.server.ServerState;
import org.jetbrains.annotations.NotNull;

import java.security.CodeSource;
import java.util.*;
import java.util.function.Consumer;

public class CraftsNetBuilder extends AbstractCraftsNetBuilder<CraftsNetBuilder> {

    private final Map<String, NamespacedBuilder<?>> internalBuilders;

    private final AddonSystemBuilder addonSystemBuilder;
    private final LoggingBuilder loggingBuilder;
    private final ServerBuilder webServerBuilder;
    private final ServerBuilder webSocketServerBuilder;

    public CraftsNetBuilder() {
        acquire(this);

        this.internalBuilders = new HashMap<>();
        this.addKnownArgument("debug", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("codeSources", ArgumentTransformer.notAllowed("Can't add code sources via the cli"));
        this.addKnownArgument("sessionCacheSize", ArgumentTransformer.integerNumber(0, Integer.MAX_VALUE));
        this.addKnownArgument("skipVersionCheck", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("tempFilesInPwd", ArgumentTransformer.booleanFlag());

        this.addonSystemBuilder = new AddonSystemBuilder(this, AddonSystemState.FULL);
        this.loggingBuilder = new LoggingBuilder(this, -1, true, false);

        this.webServerBuilder = new ServerBuilder(this, "http", ServerState.DYNAMIC, (short) 5000, false, false, false);
        this.webSocketServerBuilder = new ServerBuilder(this, "websocket", ServerState.DYNAMIC, (short) 5001, false, false, false);

        this.debug(false);
        this.sessionCacheSize(5);
        this.skipVersionCheck(false);
        this.tempFilesInPwd(false);

        this.set("codeSources", new HashSet<>());
        this.addCodeSource(this.getClass().getProtectionDomain().getCodeSource());
    }

    void registerInternalNamespace(NamespacedBuilder<?> builder) {
        this.internalBuilders.put(builder.getNamespace().toLowerCase(), builder);
    }

    public @NotNull CraftsNetBuilder setFromCli(@NotNull String... args) {
        boolean equalsSeparated = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("--argsEqualsSeparated")) {
                equalsSeparated = true;
                break;
            }
        }

        return this.setFromCli(
                args,
                equalsSeparated ? Arguments.Pattern.EQUALS_SEPARATED : Arguments.Pattern.SPACE_SEPARATED
        );
    }

    public @NotNull CraftsNetBuilder setFromCli(@NotNull String[] args, @NotNull Arguments.Pattern argsPattern) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(argsPattern, "argsPattern");

        Arguments arguments = new Arguments(args, argsPattern);
        arguments.getMapView().forEach((arg, value) -> {
            if (arg.equalsIgnoreCase("argsEqualsSeparated")) {
                return;
            }

            this.set(arg, value);
        });
        return this;
    }

    @Override
    protected @NotNull CraftsNetBuilder set(@NotNull String arg, @NotNull Object value) {
        Objects.requireNonNull(arg, "arg");

        if (!arg.contains(":")) {
            return super.set(arg, value);
        }

        String[] parts = arg.split(":", 2);
        String namespace = parts[0];

        NamespacedBuilder<?> builder = this.internalBuilders.get(namespace.toLowerCase());
        if (builder == null) {
            throw new IllegalArgumentException("No namespace " + namespace
                    + " found for argument " + arg);
        }

        builder.set(parts[1], value);
        return this;
    }

    public CraftsNetBuilder addonSystem(Consumer<AddonSystemBuilder> builderConsumer) {
        builderConsumer.accept(this.addonSystemBuilder);
        return this;
    }

    public AddonSystemBuilder addonSystem() {
        return this.addonSystemBuilder;
    }

    public CraftsNetBuilder logging(Consumer<LoggingBuilder> builderConsumer) {
        builderConsumer.accept(this.loggingBuilder);
        return this;
    }

    public LoggingBuilder logging() {
        return this.loggingBuilder;
    }

    public Collection<ServerBuilder> allServer() {
        return this.internalBuilders.entrySet().stream()
                .filter(builder -> builder instanceof ServerBuilder)
                .map(entry -> (ServerBuilder) entry.getValue())
                .toList();
    }

    public CraftsNetBuilder allServer(Consumer<ServerBuilder> builderConsumer) {
        this.allServer().forEach(builderConsumer);
        return this;
    }

    public CraftsNetBuilder webServer(Consumer<ServerBuilder> builderConsumer) {
        builderConsumer.accept(this.webServerBuilder);
        return this;
    }

    public ServerBuilder webServer() {
        return this.webServerBuilder;
    }

    public CraftsNetBuilder webSocketServer(Consumer<ServerBuilder> builderConsumer) {
        builderConsumer.accept(this.webSocketServerBuilder);
        return this;
    }

    public ServerBuilder webSocketServer() {
        return this.webSocketServerBuilder;
    }

    public CraftsNetBuilder addCodeSource(CodeSource codeSource) {
        this.getCodeSources().add(codeSource);
        return this;
    }

    public CraftsNetBuilder removeCodeSource(CodeSource codeSource) {
        this.getCodeSources().remove(codeSource);
        return this;
    }

    public Collection<CodeSource> getCodeSources() {
        return Objects.requireNonNull(
                this.getCollection("codeSources", CodeSource.class),
                "No code sources entry found"
        );
    }

    public CraftsNetBuilder debug(boolean debug) {
        return this.set("debug", debug);
    }

    public boolean debug() {
        return Objects.requireNonNull(
                this.getBoolean("debug"),
                "No debug flag found"
        );
    }

    public CraftsNetBuilder sessionCacheSize(int size) {
        return this.set("sessionCacheSize", size);
    }

    public int sessionCacheSize() {
        return Objects.requireNonNull(
                this.getInt("sessionCacheSize"),
                "No session cache size found"
        );
    }

    public CraftsNetBuilder skipVersionCheck(boolean skipVersionCheck) {
        return this.set("skipVersionCheck", skipVersionCheck);
    }

    public boolean skipVersionCheck() {
        return Objects.requireNonNull(
                this.getBoolean("skipVersionCheck"),
                "No skip version check flag found"
        );
    }

    public CraftsNetBuilder tempFilesInPwd(boolean tempFilesInPwd) {
        return this.set("tempFilesInPwd", tempFilesInPwd);
    }

    public boolean tempFilesInPwd() {
        return Objects.requireNonNull(
                this.getBoolean("tempFilesInPwd"),
                "No temp files in pwd flag found"
        );
    }

    public CraftsNet build() {
        CraftsNet craftsNet = new CraftsNet();
        craftsNet.start(this);
        return craftsNet;
    }

}
