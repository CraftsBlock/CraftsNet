package de.craftsblock.craftsnet.api.http.encoding;

import de.craftsblock.craftsnet.api.http.encoding.builtin.DeflateStreamEncoder;
import de.craftsblock.craftsnet.api.http.encoding.builtin.GZIPStreamEncoder;
import de.craftsblock.craftsnet.api.http.encoding.builtin.IdentityStreamEncoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A registry for managing and retrieving {@link StreamEncoder} instances.
 * This class is responsible for registering, unregistering, and checking the availability of stream encoders.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.3.3-SNAPSHOT
 */
public final class StreamEncoderRegistry {

    private final ConcurrentLinkedQueue<StreamEncoder> streamEncoders = new ConcurrentLinkedQueue<>();

    /**
     * Constructs a new {@link StreamEncoderRegistry} and registers the built-in stream encoders.
     */
    public StreamEncoderRegistry() {
        register(new DeflateStreamEncoder());
        register(new GZIPStreamEncoder());
        register(new IdentityStreamEncoder());
    }

    /**
     * Registers a new {@link StreamEncoder} if it is not already registered.
     *
     * @param streamEncoder The {@link StreamEncoder} to register.
     */
    public void register(@NotNull StreamEncoder streamEncoder) {
        if (this.isRegistered(streamEncoder)) return;
        streamEncoders.add(streamEncoder);
    }

    /**
     * Unregisters the given {@link StreamEncoder}.
     * The {@link IdentityStreamEncoder} encoder cannot be unregistered, as it is a default encoder.
     *
     * @param streamEncoder The {@link StreamEncoder} to unregister.
     * @throws IllegalArgumentException if trying to unregister the {@link IdentityStreamEncoder} encoder.
     */
    public void unregister(@NotNull StreamEncoder streamEncoder) {
        if (streamEncoder instanceof IdentityStreamEncoder)
            throw new IllegalArgumentException("The stream encoder " + streamEncoder.getEncodingName() + " may not be unregistered!");
        streamEncoders.remove(streamEncoder);
    }

    /**
     * Unregisters the {@link StreamEncoder} associated with the given encoding name.
     * The "identity" encoding cannot be unregistered.
     *
     * @param encoding The encoding name of the stream encoder to unregister.
     * @throws IllegalArgumentException if trying to unregister the "identity" encoder.
     */
    public void unregister(@NotNull String encoding) {
        if (encoding.equalsIgnoreCase("identity"))
            throw new IllegalArgumentException("The stream encoder " + encoding + " may not be unregistered!");

        streamEncoders.stream().map(StreamEncoder::getEncodingName)
                .filter(encoding::equalsIgnoreCase).forEach(this::unregister);
    }

    /**
     * Checks if the given {@link StreamEncoder} is already registered.
     *
     * @param streamEncoder The {@link StreamEncoder} to check.
     * @return {@code true} if the encoder is registered, {@code false} otherwise.
     */
    public boolean isRegistered(@NotNull StreamEncoder streamEncoder) {
        return streamEncoders.contains(streamEncoder);
    }

    /**
     * Checks if an {@link StreamEncoder} with the given encoding name is registered.
     *
     * @param encoding The encoding name to check.
     * @return {@code true} if the encoding is registered, {@code false} otherwise.
     */
    public boolean isRegistered(@NotNull String encoding) {
        return streamEncoders.stream().anyMatch(provider -> provider.getEncodingName().equalsIgnoreCase(encoding));
    }

    /**
     * Checks if an {@link StreamEncoder} with the given encoding name is available.
     * An encoder is considered available if it is registered and the {@link StreamEncoder#isAvailable()} method returns {@code true}.
     *
     * @param encoding The encoding name to check.
     * @return {@code true} if the encoder is available, {@code false} otherwise.
     */
    public boolean isAvailable(@NotNull String encoding) {
        return streamEncoders.stream().filter(provider -> provider.getEncodingName().equalsIgnoreCase(encoding))
                .anyMatch(StreamEncoder::isAvailable);
    }

    /**
     * Retrieves a {@link StreamEncoder} by its encoding name.
     * The encoder must be available, meaning it is registered and {@link StreamEncoder#isAvailable()} returns {@code true}.
     *
     * @param encoding The encoding name of the encoder to retrieve.
     * @return The {@link StreamEncoder} for the given encoding name, or {@code null} if no available encoder is found.
     */
    public @Nullable StreamEncoder retrieveEncoder(@NotNull String encoding) {
        return streamEncoders.stream().filter(provider -> provider.getEncodingName().equalsIgnoreCase(encoding))
                .filter(StreamEncoder::isAvailable).findFirst().orElse(null);
    }

    /**
     * Retrieves a {@link StreamEncoder} by its type.
     * The encoder must be available, meaning it is registered and {@link StreamEncoder#isAvailable()} returns {@code true}.
     *
     * @param type The class type of the encoder to retrieve.
     * @return The {@link StreamEncoder} of the given type, or {@code null} if no available encoder of that type is found.
     */
    public @Nullable StreamEncoder retrieveEncoder(@NotNull Class<? extends StreamEncoder> type) {
        return streamEncoders.stream().filter(provider -> type.isInstance(type))
                .filter(StreamEncoder::isAvailable).findFirst().orElse(null);
    }

}
