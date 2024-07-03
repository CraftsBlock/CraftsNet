package de.craftsblock.craftsnet.api.websocket.extensions.builtin;

import de.craftsblock.craftsnet.api.websocket.Frame;
import de.craftsblock.craftsnet.api.websocket.Opcode;
import de.craftsblock.craftsnet.api.websocket.extensions.WebSocketExtension;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * WebSocket extension that implements per-message deflate compression as described in
 * <a href="https://tools.ietf.org/html/rfc7692">RFC 7692</a>. This extension allows for
 * compressing and decompressing WebSocket frames.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.6-SNAPSHOT
 */
public class PerMessageDeflateExtension extends WebSocketExtension {

    private static final byte PADDING_BYTE = 0x00;
    private static final byte[] BLOCK_END_BYTES = new byte[]{0x00, 0x00, (byte) 0xff, (byte) 0xff};

    private static int compressWhenHigher = 512;
    private static int maxDataLength = 100 * 1024 * 1024;

    private static int deflateLevel = Deflater.DEFAULT_COMPRESSION;
    private static int deflateStrategy = Deflater.DEFAULT_STRATEGY;
    private static int deflateFlush = Deflater.NO_FLUSH;

    /**
     * Constructs a new {@code PerMessageDeflateExtension} with the protocol name "permessage-deflate".
     */
    public PerMessageDeflateExtension() {
        super("permessage-deflate");
    }

    /**
     * Encodes a WebSocket frame using per-message deflate compression. Control frames are not compressed,
     * and frames smaller than the specified threshold are not compressed.
     *
     * @param frame the frame to be encoded. Must not be null.
     * @return the compressed frame if applicable, or the original frame if no compression was applied.
     */
    @Override
    public @NotNull Frame encode(@NotNull Frame frame) {
        if (frame.getOpcode().isControlCode() || frame.isRsv1() || frame.getData().length < compressWhenHigher)
            return frame;

        Frame result = new Frame(
                frame.isFinalFrame(),
                !frame.getOpcode().equals(Opcode.CONTINUATION),
                frame.isRsv2(), frame.isRsv3(), frame.getOpcode(), new byte[0]
        );

        Deflater deflater = new Deflater(deflateLevel, true);
        deflater.setStrategy(deflateStrategy);
        deflater.setInput(frame.getData());
        deflater.finish();

        byte[] buffer = new byte[1024];
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            while (!deflater.finished()) {
                int length = deflater.deflate(buffer, 0, buffer.length, deflateFlush);
                stream.write(buffer, 0, length);
            }

            if (!frame.isFinalFrame()) result.setData(stream.toByteArray());
            else {
                byte[] data = stream.toByteArray();
                byte[] merged = new byte[data.length + 1];
                System.arraycopy(data, 0, merged, 0, data.length);
                System.arraycopy(new byte[]{PADDING_BYTE}, 0, merged, data.length, 1);
                result.setData(merged);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deflater.end();
        }

        return result;
    }

    /**
     * Decodes a WebSocket frame using per-message deflate decompression. Control frames and frames
     * without the RSV1 bit set are not decompressed.
     *
     * @param frame the frame to be decoded. Must not be null.
     * @return the decompressed frame if applicable, or the original frame if no decompression was applied.
     */
    @Override
    public @NotNull Frame decode(@NotNull Frame frame) {
        if (frame.getOpcode().isControlCode() || !frame.isRsv1())
            return frame;

        Frame result = new Frame(frame.isFinalFrame(), false, frame.isRsv2(), frame.isRsv3(), frame.getOpcode(), new byte[0]);
        Inflater inflater = new Inflater(true);
        inflater.setInput(frame.getData());

        byte[] buffer = new byte[1024];
        int totalLength = 0;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            while (!inflater.finished()) {
                if (inflater.getRemaining() == 0) break;
                int length = inflater.inflate(buffer);

                totalLength += length;
                if (totalLength > maxDataLength)
                    throw new IllegalStateException("Decompressed data exceeds the maximum permitted size! (Max size: "
                            + (maxDataLength / 1024 / 1024) + " MB)");

                stream.write(buffer, 0, length);
            }

            result.setData(stream.toByteArray());
        } catch (IOException | IllegalStateException | DataFormatException e) {
            throw new RuntimeException(e);
        } finally {
            inflater.end();
        }

        return result;
    }

    /**
     * Sets the threshold for compression. Frames with data length less than this value will not be compressed.
     *
     * @param compressWhenHigher the new threshold for compression in bytes.
     */
    public static void setCompressWhenHigher(int compressWhenHigher) {
        PerMessageDeflateExtension.compressWhenHigher = compressWhenHigher;
    }

    /**
     * Gets the threshold for compression.
     *
     * @return the threshold for compression in bytes.
     */
    public static int getCompressWhenHigher() {
        return compressWhenHigher;
    }

    /**
     * Sets the maximum allowed data length for decompressed data. Exceeding this limit will cause an exception.
     *
     * @param maxDataLength the new maximum data length in bytes.
     */
    public static void setMaxDataLength(int maxDataLength) {
        PerMessageDeflateExtension.maxDataLength = maxDataLength;
    }

    /**
     * Gets the maximum allowed data length for decompressed data.
     *
     * @return the maximum data length in bytes.
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

    /**
     * Sets the deflate flush mode.
     *
     * @param deflateFlush the new flush mode.
     */
    public static void setDeflateFlush(int deflateFlush) {
        PerMessageDeflateExtension.deflateFlush = deflateFlush;
    }

    /**
     * Gets the deflate flush mode.
     *
     * @return the flush mode.
     */
    public static int getDeflateFlush() {
        return deflateFlush;
    }

}
