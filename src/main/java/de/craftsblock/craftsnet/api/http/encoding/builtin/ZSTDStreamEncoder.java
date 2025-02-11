package de.craftsblock.craftsnet.api.http.encoding.builtin;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A concrete implementation of {@link StreamEncoder} that performs compression and decompression
 * using the Zstandard algorithm.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.3-SNAPSHOT
 */
public final class ZSTDStreamEncoder extends StreamEncoder {

    /**
     * Constructs a new {@link ZSTDStreamEncoder}.
     */
    public ZSTDStreamEncoder() {
        super("zstd");
    }

    /**
     * Encodes the provided {@link OutputStream} by wrapping it in a {@link ZstdOutputStream}.
     *
     * @param raw The raw output stream to be encoded.
     * @return The encoded output stream.
     * @throws RuntimeException if an {@link IOException} occurs while creating the {@link ZstdOutputStream}.
     */
    @Override
    public @NotNull OutputStream encodeOutputStream(@NotNull OutputStream raw) {
        try {
            return new ZstdOutputStream(super.encodeOutputStream(raw));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the provided {@link InputStream} by wrapping it in a {@link ZstdInputStream}.
     *
     * @param raw The raw input stream to be encoded.
     * @return The encoded input stream.
     * @throws RuntimeException if an {@link IOException} occurs while creating the {@link ZstdInputStream}.
     */
    @Override
    public @NotNull InputStream encodeInputStream(@NotNull InputStream raw) {
        try {
            return new ZstdInputStream(super.encodeInputStream(raw));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
