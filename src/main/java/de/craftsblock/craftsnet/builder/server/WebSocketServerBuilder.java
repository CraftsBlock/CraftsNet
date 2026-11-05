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
        this.addKnownArgument("maxFrameSize", ArgumentTransformer.integerNumber(1, Integer.MAX_VALUE));
        this.addKnownArgument("readTimeout", ArgumentTransformer.integerNumber(0, Integer.MAX_VALUE));

        this.fragment(false);
        this.fragmentSize(1024);
        this.maxFrameSize(1024 * 1024);
        this.readTimeout(60_000);

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

    /**
     * Sets the maximum permitted size of a WebSocket frame.
     * <p>
     * The specified value must be greater than or equal to {@code 1}.
     *
     * @param maxFrameSize the maximum frame size in bytes
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the value is less than {@code 1}
     */
    public WebSocketServerBuilder maxFrameSize(int maxFrameSize) {
        ArgumentTransformer.validatedNumber(maxFrameSize, 1, Integer.MAX_VALUE);
        return this.set("maxFrameSize", maxFrameSize);
    }

    /**
     * Returns the configured maximum permitted WebSocket frame size.
     *
     * @return the maximum frame size in bytes
     * @throws NullPointerException if no maximum frame size is configured
     */
    public int maxFrameSize() {
        return Objects.requireNonNull(
                this.getInt("maxFrameSize"),
                "No max frame size found for the web socket server"
        );
    }

    /**
     * Sets the timeout for reading data from a WebSocket connection.
     * <p>
     * The specified value must be greater than or equal to {@code 0}.
     *
     * @param readTimeout the read timeout in milliseconds
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the value is negative
     */
    public WebSocketServerBuilder readTimeout(int readTimeout) {
        ArgumentTransformer.validatedNumber(readTimeout, 0, Integer.MAX_VALUE);
        return this.set("readTimeout", readTimeout);
    }

    /**
     * Returns the configured WebSocket read timeout.
     *
     * @return the read timeout in milliseconds
     * @throws NullPointerException if no read timeout is configured
     */
    public int readTimeout() {
        return Objects.requireNonNull(
                this.getInt("readTimeout"),
                "No read timeout found for the web socket server"
        );
    }

}
