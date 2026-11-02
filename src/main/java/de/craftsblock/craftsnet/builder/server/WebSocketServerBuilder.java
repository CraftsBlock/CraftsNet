package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WebSocketServerBuilder extends ServerBuilder<WebSocketServerBuilder> {

    public WebSocketServerBuilder(@NotNull CraftsNetBuilder root, String namespace, int port) {
        super(root, namespace, port);
        this.acquire(this);

        this.addKnownArgument("fragment", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("fragmentSize", ArgumentTransformer.integerNumber(1, Integer.MAX_VALUE));

        this.fragment(false);
        this.fragmentSize(1024);
    }

    public WebSocketServerBuilder fragment(boolean fragment) {
        this.set("fragment", fragment);
        return this;
    }

    public boolean fragment() {
        return Objects.requireNonNull(
                this.getBoolean("fragment"),
                "No fragment flag found for the web socket server"
        );
    }

    public WebSocketServerBuilder fragmentSize(int fragmentSize) {
        ArgumentTransformer.validatedNumber(fragmentSize, 1, Integer.MAX_VALUE);
        this.set("fragmentSize", fragmentSize);
        return this;
    }

    public int fragmentSize() {
        return Objects.requireNonNull(
                this.getInt("fragmentSize"),
                "No fragment size found for the web socket server"
        );
    }

}
