package de.craftsblock.craftsnet.api.websocket.extensions.builtin;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.api.websocket.WebSocketClient;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtension;
import de.craftsblock.craftsnet.events.sockets.ClientDisconnectEvent;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * WebSocket extension that implements per-message deflate compression as described in
 * <a href="https://tools.ietf.org/html/rfc7692">RFC 7692</a>.
 *
 * <p>This implementation reuses {@link Deflater} and {@link Inflater} instances for each
 * WebSocket connection. Their native resources are therefore not allocated and released
 * for every individual frame.
 *
 * <p>By default, compression and decompression contexts are reset after each complete
 * message, corresponding to the no-context-takeover behavior. This can be changed
 * when context-takeover parameters have been negotiated.
 *
 * <p>Fragmented messages are processed using the same compression context until the
 * final fragment is received.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.6-SNAPSHOT
 */
public class PerMessageDeflateExtension extends WebSocketExtension {

    private static final byte[] BLOCK_END_BYTES = new byte[]{
            0x00, 0x00, (byte) 0xff, (byte) 0xff
    };

    private static final int BUFFER_SIZE = 8192;

    private static int compressWhenHigher = 512;
    private static int maxDataLength = 100 * 1024 * 1024;

    private static int deflateLevel = Deflater.DEFAULT_COMPRESSION;
    private static int deflateStrategy = Deflater.DEFAULT_STRATEGY;

    /**
     * Constructs a new {@code PerMessageDeflateExtension} with the protocol name
     * "permessage-deflate".
     */
    public PerMessageDeflateExtension(CraftsNet craftsNet) {
        super("permessage-deflate");

        craftsNet.getListenerRegistry().register(
                ClientDisconnectEvent.class,
                (event) -> this.release(event.getClient()),
                false
        );
    }

    /**
     * Holds the compression and decompression state of a single WebSocket connection.
     *
     * <p>Keeping both instances together also allows fragmented messages to share
     * their respective compression contexts.
     */
    private static final class CompressionContext {

        private final Deflater deflater = new Deflater(deflateLevel, true);
        private final Inflater inflater = new Inflater(true);

        private boolean outgoingCompressed;
        private boolean incomingCompressed;

        private int totalDecodedLength;

        private boolean released;

        /**
         * Releases the native resources allocated by the compression engines.
         */
        private void release() {
            if (released)
                return;

            deflater.end();
            inflater.end();

            released = true;
        }

    }

    /**
     * Retrieves the compression context associated with the given client.
     *
     * <p>A new context is created if none exists or if its previous resources
     * have already been released.
     *
     * @param client the WebSocket client.
     * @return the client's compression context.
     */
    private CompressionContext getContext(@NotNull WebSocketClient client) {
        CompressionContext context = client.getContext().getTyped(CompressionContext.class);

        if (context == null || context.released) {
            context = new CompressionContext();
            client.getContext().put(context);
        }

        return context;
    }

    /**
     * Encodes a WebSocket frame using per-message deflate compression.
     *
     * <p>Control frames are never compressed. The compression context is reused
     * across frames belonging to the same message.
     *
     * @param client the client to encode the frame for.
     * @param frame  the frame to be encoded.
     * @return the compressed frame if applicable, or the original frame.
     */
    @Override
    public @NotNull Frame encode(
            @NotNull WebSocketClient client,
            @NotNull Frame frame
    ) {
        if (frame.getOpcode().isControlCode() || frame.isRsv1()) {
            return frame;
        }

        CompressionContext context = getContext(client);

        boolean continuation = frame.getOpcode().equals(Opcode.CONTINUATION);
        if (!continuation) {
            if (frame.getData().length < compressWhenHigher) {
                context.outgoingCompressed = false;
                return frame;
            }

            context.outgoingCompressed = true;

            context.deflater.reset();
            context.deflater.setLevel(deflateLevel);
            context.deflater.setStrategy(deflateStrategy);
        } else if (!context.outgoingCompressed) {
            return frame;
        }

        Deflater deflater = context.deflater;
        deflater.setInput(frame.getData());

        byte[] buffer = new byte[BUFFER_SIZE];
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(
                frame.getData().length + 64
        )) {
            int flushMode = frame.isFinalFrame()
                    ? Deflater.SYNC_FLUSH
                    : Deflater.NO_FLUSH;

            int length;
            do {
                length = deflater.deflate(
                        buffer,
                        0,
                        buffer.length,
                        flushMode
                );

                if (length > 0)
                    stream.write(buffer, 0, length);

            } while (!deflater.needsInput()
                    || (flushMode == Deflater.SYNC_FLUSH
                    && length == buffer.length));

            byte[] compressed = stream.toByteArray();
            if (frame.isFinalFrame()) {
                if (compressed.length < BLOCK_END_BYTES.length
                        || !endsWith(compressed, BLOCK_END_BYTES)) {
                    throw new IllegalStateException(
                            "Deflater did not produce the expected SYNC_FLUSH trailer!"
                    );
                }

                compressed = Arrays.copyOf(
                        compressed,
                        compressed.length - BLOCK_END_BYTES.length
                );

                context.outgoingCompressed = false;

                deflater.reset();
            }

            return new Frame(
                    frame.isFinalFrame(),
                    !continuation,
                    frame.isRsv2(),
                    frame.isRsv3(),
                    frame.isMasked(),
                    frame.getOpcode(),
                    compressed
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decodes a WebSocket frame using per-message deflate decompression.
     *
     * <p>Fragmented messages are decompressed using the same inflater instance.
     * The RFC 7692 synchronization trailer is appended when the final fragment
     * is received.
     *
     * @param client the client to decode the frame for.
     * @param frame  the frame to be decoded.
     * @return the decompressed frame if applicable, or the original frame.
     */
    @Override
    public @NotNull Frame decode(
            @NotNull WebSocketClient client,
            @NotNull Frame frame
    ) {
        if (frame.getOpcode().isControlCode()) {
            return frame;
        }

        CompressionContext context = getContext(client);
        if (frame.getOpcode().isDataCode()) {
            if (!frame.isRsv1()) {
                return frame;
            }

            context.incomingCompressed = true;
            context.totalDecodedLength = 0;

            context.inflater.reset();
        } else if (!context.incomingCompressed) {
            return frame;
        }

        Inflater inflater = context.inflater;
        byte[] input = frame.getData();
        if (frame.isFinalFrame()) {
            byte[] merged = Arrays.copyOf(
                    input,
                    input.length + BLOCK_END_BYTES.length
            );

            System.arraycopy(
                    BLOCK_END_BYTES,
                    0,
                    merged,
                    input.length,
                    BLOCK_END_BYTES.length
            );

            input = merged;
        }

        inflater.setInput(input);

        byte[] buffer = new byte[BUFFER_SIZE];
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(
                Math.max(32, input.length)
        )) {
            while (!inflater.needsInput()) {
                int length = inflater.inflate(buffer);
                if (length > 0) {
                    context.totalDecodedLength += length;

                    if (context.totalDecodedLength > maxDataLength) {
                        throw new IllegalStateException(
                                "Decompressed data exceeds the maximum permitted size! (Max size: "
                                        + (maxDataLength / 1024 / 1024)
                                        + " MB)"
                        );
                    }

                    stream.write(buffer, 0, length);
                } else if (inflater.needsDictionary()) {
                    throw new IllegalStateException(
                            "Inflater requires a preset dictionary!"
                    );
                } else if (!inflater.needsInput() && !inflater.finished()) {
                    throw new IllegalStateException(
                            "Inflater could not make progress!"
                    );
                }
            }

            if (frame.isFinalFrame()) {
                context.incomingCompressed = false;
                context.totalDecodedLength = 0;

                inflater.reset();
            }

            return new Frame(
                    frame.isFinalFrame(),
                    false,
                    frame.isRsv2(),
                    frame.isRsv3(),
                    frame.isMasked(),
                    frame.getOpcode(),
                    stream.toByteArray()
            );
        } catch (DataFormatException | IOException e) {
            throw new RuntimeException(
                    "Failed to decompress WebSocket frame!",
                    e
            );
        }
    }

    /**
     * Checks whether the given byte array ends with the specified suffix.
     *
     * @param data   the byte array to inspect.
     * @param suffix the expected suffix.
     * @return {@code true} if the array ends with the suffix.
     */
    private static boolean endsWith(byte[] data, byte[] suffix) {
        if (data.length < suffix.length) {
            return false;
        }

        int offset = data.length - suffix.length;
        for (int i = 0; i < suffix.length; i++) {
            if (data[offset + i] != suffix[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Releases the compression and decompression resources associated with a client.
     *
     * <p>This method should be called when the WebSocket connection is closed.
     * The context will be recreated automatically if the client is used again.
     *
     * @param client the client whose compression resources should be released.
     */
    public void release(@NotNull WebSocketClient client) {
        CompressionContext context = client.getContext()
                .getTyped(CompressionContext.class);

        if (context != null) {
            context.release();
        }
    }

    /**
     * Sets the threshold for compression. Frames with data length less than
     * this value will not initiate compression.
     *
     * @param compressWhenHigher the new threshold in bytes.
     */
    public static void setCompressWhenHigher(int compressWhenHigher) {
        PerMessageDeflateExtension.compressWhenHigher = compressWhenHigher;
    }

    /**
     * Gets the threshold for compression.
     *
     * @return the compression threshold in bytes.
     */
    public static int getCompressWhenHigher() {
        return compressWhenHigher;
    }

    /**
     * Sets the maximum allowed decompressed message size.
     *
     * @param maxDataLength the maximum decompressed size in bytes.
     */
    public static void setMaxDataLength(int maxDataLength) {
        PerMessageDeflateExtension.maxDataLength = maxDataLength;
    }

    /**
     * Gets the maximum allowed decompressed message size.
     *
     * @return the maximum decompressed message size in bytes.
     */
    public static int getMaxDataLength() {
        return maxDataLength;
    }

    /**
     * Sets the deflate compression level.
     *
     * @param deflateLevel the new compression level.
     */
    public static void setDeflateLevel(int deflateLevel) {
        PerMessageDeflateExtension.deflateLevel = deflateLevel;
    }

    /**
     * Gets the deflate compression level.
     *
     * @return the compression level.
     */
    public static int getDeflateLevel() {
        return deflateLevel;
    }

    /**
     * Sets the deflate compression strategy.
     *
     * @param deflateStrategy the new compression strategy.
     */
    public static void setDeflateStrategy(int deflateStrategy) {
        PerMessageDeflateExtension.deflateStrategy = deflateStrategy;
    }

    /**
     * Gets the deflate compression strategy.
     *
     * @return the compression strategy.
     */
    public static int getDeflateStrategy() {
        return deflateStrategy;
    }

}
