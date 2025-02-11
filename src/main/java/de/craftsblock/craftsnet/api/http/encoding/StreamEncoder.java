package de.craftsblock.craftsnet.api.http.encoding;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Abstract class representing a stream encoder, which can be used to encode and decode input and output streams.
 * Subclasses should implement specific encoding mechanisms as needed.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.3-SNAPSHOT
 */
public abstract class StreamEncoder {

    private final String encodingName;

    /**
     * Constructs a new {@code StreamEncoder} with the specified encoding name.
     *
     * @param encodingName The name of the encoding method; must not be null.
     */
    public StreamEncoder(@NotNull String encodingName) {
        this.encodingName = encodingName;
    }

    /**
     * Encodes the provided {@link OutputStream}. By default, this method returns the raw stream.
     * Subclasses should override this method to provide actual encoding functionality.
     *
     * @param raw The raw output stream to be encoded; must not be null.
     * @return The encoded output stream; by default, returns the same raw stream.
     */
    public @NotNull OutputStream encodeOutputStream(@NotNull OutputStream raw) {
        return raw;
    }

    /**
     * Encodes the provided {@link InputStream}. By default, this method returns the raw stream.
     * Subclasses should override this method to provide actual encoding functionality.
     *
     * @param raw The raw input stream to be encoded; must not be null.
     * @return The encoded input stream; by default, returns the same raw stream.
     */
    public @NotNull InputStream encodeInputStream(@NotNull InputStream raw) {
        return raw;
    }

    /**
     * Checks whether the encoding method is available.
     * Subclasses may override this method to implement specific availability checks.
     *
     * @return {@code true} if the encoding method is available; default is {@code true}.
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * Gets the name of the encoding method used by this encoder.
     *
     * @return The encoding name; never null.
     */
    public final @NotNull String getEncodingName() {
        return encodingName;
    }

    /**
     * Compares this encoder to another object for equality. Two encoders are considered equal
     * if they have the same encoding name.
     *
     * @param o The object to compare to.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        StreamEncoder that = (StreamEncoder) o;
        return Objects.equals(this.getEncodingName(), that.getEncodingName());
    }

    /**
     * Computes a hash code for this encoder based on its encoding name.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getEncodingName());
    }

}
