package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.ArgumentTransformer;
import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Builder for configuring the WebSocket server.
 * <p>
 * This class extends {@link ServerBuilder} with WebSocket-specific settings, including
 * message fragmentation and the maximum fragment size.
 * <p>
 * The builder uses the {@code websocket} namespace and initializes its own default
 * configuration as well as the inherited server settings.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see ServerBuilder
 * @see ServerState
 * @see ArgumentTransformer
 * @since 4.0.0
 */
public class WebSocketServerBuilder extends ServerBuilder<WebSocketServerBuilder> {

    /**
     * Creates and initializes the WebSocket server builder.
     * <p>
     * The constructor assigns the {@code websocket} namespace, registers this builder
     * with the root builder, registers the WebSocket-specific arguments, and applies
     * the default configuration.
     *
     * @param root the root CraftsNet builder this WebSocket server builder belongs to
     */
    public WebSocketServerBuilder(@NotNull CraftsNetBuilder root) {
        super(root, "websocket");
        this.acquire(this);

        this.addKnownArgument("fragment", ArgumentTransformer.booleanFlag());
        this.addKnownArgument("fragmentSize", ArgumentTransformer.integerNumber(1, Integer.MAX_VALUE));

        this.fragment(false);
        this.fragmentSize(1024);

        this.backlog(0);
        this.maxUriLength(4096);
        this.port(5001);
        this.responseEncoding(false);
        this.skipDefaultRoute(false);
        this.ssl(false);
        this.state(ServerState.DYNAMIC);
    }

    /**
     * Enables or disables WebSocket message fragmentation.
     *
     * @param fragment {@code true} to enable fragmentation; {@code false} to disable it
     * @return this builder instance for method chaining
     */
    public WebSocketServerBuilder fragment(boolean fragment) {
        this.set("fragment", fragment);
        return this;
    }

    /**
     * Retrieves whether WebSocket message fragmentation is enabled.
     *
     * @return {@code true} if fragmentation is enabled; otherwise {@code false}
     * @throws NullPointerException if no fragmentation flag is configured
     */
    public boolean fragment() {
        return Objects.requireNonNull(
                this.getBoolean("fragment"),
                "No fragment flag found for the web socket server"
        );
    }

    /**
     * Sets the maximum size of an individual WebSocket fragment.
     *
     * @param fragmentSize the maximum fragment size, which must be at least {@code 1}
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the fragment size is less than {@code 1}
     */
    public WebSocketServerBuilder fragmentSize(int fragmentSize) {
        ArgumentTransformer.validatedNumber(fragmentSize, 1, Integer.MAX_VALUE);
        this.set("fragmentSize", fragmentSize);
        return this;
    }

    /**
     * Retrieves the configured maximum WebSocket fragment size.
     *
     * @return the configured maximum fragment size
     * @throws NullPointerException if no fragment size is configured
     */
    public int fragmentSize() {
        return Objects.requireNonNull(
                this.getInt("fragmentSize"),
                "No fragment size found for the web socket server"
        );
    }

}
