package de.craftsblock.craftsnet.api.http.encoding.builtin;

import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A concrete implementation of {@link StreamEncoder} that performs compression and decompression
 * using the GZIP algorithm.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.3-SNAPSHOT
 */
public final class GZIPStreamEncoder extends StreamEncoder {

    /**
     * Constructs a new {@link GZIPStreamEncoder}.
     */
    public GZIPStreamEncoder() {
        super("gzip");
    }

    /**
     * Encodes the provided {@link OutputStream} by wrapping it in a {@link GZIPOutputStream}.
     *
     * @param raw The raw output stream to be encoded.
     * @return The encoded output stream.
     * @throws RuntimeException if an {@link IOException} occurs while creating the {@link GZIPOutputStream}.
     */
    @Override
    public @NotNull OutputStream encodeOutputStream(@NotNull OutputStream raw) {
        try {
            return new GZIPOutputStream(super.encodeOutputStream(raw));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the provided {@link InputStream} by wrapping it in a {@link GZIPInputStream}.
     *
     * @param raw The raw input stream to be encoded.
     * @return The encoded input stream.
     * @throws RuntimeException if an {@link IOException} occurs while creating the {@link GZIPInputStream}.
     */
    @Override
    public @NotNull InputStream encodeInputStream(@NotNull InputStream raw) {
        try {
            return new GZIPInputStream(super.encodeInputStream(raw));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
