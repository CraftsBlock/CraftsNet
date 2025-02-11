package de.craftsblock.craftsnet.api.http.encoding.builtin;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import de.craftsblock.craftsnet.api.http.encoding.StreamEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A concrete implementation of {@link StreamEncoder} that performs compression and decompression
 * using the Brotli algorithm.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.3-SNAPSHOT
 */
public final class BrotliStreamEncoder extends StreamEncoder {

    // Static block to initialize Brotli support.
    static {
        BrotliLoader.isBrotliAvailable();
    }

    /**
     * Constructs a new {@link BrotliLoader}.
     */
    public BrotliStreamEncoder() {
        super("br");
    }

    /**
     * Encodes the provided {@link OutputStream} by wrapping it in a {@link BrotliOutputStream}.
     *
     * @param raw The raw output stream to be encoded.
     * @return The encoded output stream.
     * @throws RuntimeException if an {@link IOException} occurs while creating the {@link BrotliOutputStream}.
     */
    @Override
    public @NotNull OutputStream encodeOutputStream(@NotNull OutputStream raw) {
        try {
            return new BrotliOutputStream(super.encodeOutputStream(raw));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the provided {@link InputStream} by wrapping it in a {@link BrotliInputStream}.
     *
     * @param raw The raw input stream to be encoded.
     * @return The encoded input stream.
     * @throws RuntimeException if an {@link IOException} occurs while creating the {@link BrotliInputStream}.
     */
    @Override
    public @NotNull InputStream encodeInputStream(@NotNull InputStream raw) {
        try {
            return new BrotliInputStream(super.encodeInputStream(raw));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if Brotli compression is available.
     *
     * @return {@code true} if Brotli compression is available, {@code false} otherwise.
     */
    @Override
    public boolean isAvailable() {
        return BrotliLoader.isBrotliAvailable();
    }

}
