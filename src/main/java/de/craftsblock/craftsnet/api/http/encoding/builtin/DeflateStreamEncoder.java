package de.craftsblock.craftsnet.api.http.encoding.builtin;

import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * A concrete implementation of {@link StreamEncoder} that performs compression and decompression
 * using the Deflate algorithm.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.3-SNAPSHOT
 */
public final class DeflateStreamEncoder extends StreamEncoder {

    /**
     * Constructs a new {@link DeflateStreamEncoder}.
     */
    public DeflateStreamEncoder() {
        super("deflate");
    }

    /**
     * Encodes the provided {@link OutputStream} by wrapping it in a {@link DeflaterOutputStream}.
     *
     * @param raw The raw output stream to be encoded.
     * @return The encoded output stream.
     */
    @Override
    public @NotNull OutputStream encodeOutputStream(@NotNull OutputStream raw) {
        return new DeflaterOutputStream(super.encodeOutputStream(raw));
    }

    /**
     * Encodes the provided {@link InputStream} by wrapping it in a {@link DeflaterInputStream}.
     *
     * @param raw The raw input stream to be encoded.
     * @return The encoded input stream.
     */
    @Override
    public @NotNull InputStream encodeInputStream(@NotNull InputStream raw) {
        return new DeflaterInputStream(super.encodeInputStream(raw));
    }

}