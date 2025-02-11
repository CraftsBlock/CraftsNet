package de.craftsblock.craftsnet.api.http.encoding.builtin;

import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A concrete implementation of {@link StreamEncoder} that performs no encoding
 * and returns the input and output streams as they are.
 * This is commonly referred to as the "identity" encoding, which means no transformation
 * is applied to the stream data.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.3-SNAPSHOT
 */
public final class IdentityStreamEncoder extends StreamEncoder {

    /**
     * Constructs a new {@link IdentityStreamEncoder}.
     * The encoding name is set to "identity", indicating no encoding is applied.
     */
    public IdentityStreamEncoder() {
        super("identity");
    }

    /**
     * Returns the raw output stream without applying any encoding.
     * This implementation simply delegates to the superclass method without modification.
     *
     * @param raw The raw output stream to encode.
     * @return The raw output stream.
     */
    @Override
    public @NotNull OutputStream encodeOutputStream(@NotNull OutputStream raw) {
        return super.encodeOutputStream(raw);
    }

    /**
     * Returns the raw input stream without applying any encoding.
     * This implementation simply delegates to the superclass method without modification.
     *
     * @param raw The raw input stream to encode.
     * @return The raw input stream.
     */
    @Override
    public @NotNull InputStream encodeInputStream(@NotNull InputStream raw) {
        return super.encodeInputStream(raw);
    }

}
