package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import de.craftsblock.craftsnet.builder.NamespacedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ServerBuilder extends NamespacedBuilder<ServerBuilder> {

    public ServerBuilder(@NotNull CraftsNetBuilder root, String namespace, @NotNull ServerState state,
                         short port, boolean ssl, boolean skipDefaultRoute, boolean responseEncoding) {
        super(root, namespace);
        this.acquire(this);

        this.addKnownArgument("port", ArgumentTransformer.integerNumber(0, 65365));
        this.addKnownArgument("responseEncoding", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("skipDefaultRoute", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("ssl", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("state", ArgumentTransformer.enumType(ServerState.class));
        this.addKnownArgument("trustedProxyHeader", ArgumentTransformer.list());

        this.port(port);
        this.responseEncoding(responseEncoding);
        this.skipDefaultRoute(skipDefaultRoute);
        this.ssl(ssl);
        this.state(state);
    }

    public @NotNull ServerBuilder port(int port) {
        return this.set("port", port);
    }

    public int port() {
        return Objects.requireNonNull(
                this.getInt("port"),
                "No port found for the " + getNamespace()
        );
    }

    public boolean isPort(int port) {
        return this.port() == port;
    }

    public @NotNull ServerBuilder responseEncoding(boolean responseEncoding) {
        return this.set("responseEncoding", responseEncoding);
    }

    public boolean responseEncoding() {
        return Objects.requireNonNull(
                this.getBoolean("responseEncoding"),
                "No response encoding flag found for " + this.getNamespace()
        );
    }

    public @NotNull ServerBuilder skipDefaultRoute(boolean skipDefaultRoute) {
        return this.set("skipDefaultRoute", skipDefaultRoute);
    }

    public boolean skipDefaultRoute() {
        return Objects.requireNonNull(
                this.getBoolean("skipDefaultRoute"),
                "No skip default route state found for " + getNamespace()
        );
    }

    public @NotNull ServerBuilder ssl(boolean ssl) {
        return this.set("ssl", ssl);
    }

    public boolean ssl() {
        return Objects.requireNonNull(
                this.getBoolean("ssl"),
                "No ssl state found for " + getNamespace()
        );
    }

    public @NotNull ServerBuilder state(@NotNull ServerState state) {
        return this.set("state", state);
    }

    public @NotNull ServerState state() {
        return Objects.requireNonNull(
                this.getEnum("state", ServerState.class),
                "No state found for " + getNamespace()
        );
    }

    public boolean isState(@NotNull ServerState state) {
        return this.state() == state;
    }

    public @NotNull ServerBuilder trustedProxyHeader(@NotNull String... trustedProxyHeader) {
        return this.trustedProxyHeader(List.of(trustedProxyHeader));
    }

    public @NotNull ServerBuilder trustedProxyHeader(@NotNull Collection<String> trustedProxyHeader) {
        return this.set("trustedProxyHeader", trustedProxyHeader);
    }

    public Collection<String> trustedProxyHeader() {
        return Objects.requireNonNullElse(
                this.getCollection("trustedProxyHeader", String.class),
                Collections.emptyList()
        );
    }

}
