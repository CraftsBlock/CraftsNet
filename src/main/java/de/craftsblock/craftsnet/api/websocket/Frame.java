package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represents a WebSocket frame that contains control information and payload data.
 * This class handles parsing of WebSocket frames, managing frame attributes,
 * and processing the frame data according to the WebSocket protocol.
 * <p>
 * A WebSocket frame consists of (as described in <a href="https://datatracker.ietf.org/doc/html/rfc6455">RFC 6455</a>):
 * <ul>
 *     <li>A FIN bit indicating if this is the final frame in a sequence of fragmented frames.</li>
 *     <li>RSV1, RSV2, and RSV3 bits for protocol-specific extensions which are typically set to zero.</li>
 *     <li>An opcode that defines the type of the frame (e.g., text, binary, close, ping, pong).</li>
 *     <li>Payload length indicating the size of the payload data.</li>
 *     <li>Masking key used to mask the payload data for security purposes.</li>
 *     <li>Payload data containing the actual message or control information.</li>
 * </ul>
 * <p>
 * This class supports reading frames from an input stream, handling fragmented frames,
 * and decoding the payload data. It also provides methods to access frame attributes
 * and manipulate the payload data.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.2
 * @since 3.0.6-SNAPSHOT
 */
public class Frame implements RequireAble {

    private Opcode opcode;
    private ByteBuffer data;

    private boolean fin;
    private final boolean rsv1, rsv2, rsv3;
    private boolean masked;

    /**
     * Constructs a new {@code Frame} with the specified parameters. This constructor initializes the frame
     * with the provided values for the final frame indicator, reserved bits, opcode, and payload data.
     *
     * @param fin    whether this frame is the final fragment in a message. If true, this is the final frame.
     * @param rsv1   the first reserved bit. Should be false unless an extension uses it.
     * @param rsv2   the second reserved bit. Should be false unless an extension uses it.
     * @param rsv3   the third reserved bit. Should be false unless an extension uses it.
     * @param masked whether the frame was masked, when it was received.
     * @param opcode the opcode of the frame, indicating the type of frame.
     * @param data   the payload data of the frame. This array should not be null and contains the actual message data.
     */
    public Frame(boolean fin, boolean rsv1, boolean rsv2, boolean rsv3, boolean masked, @NotNull Opcode opcode, byte @NotNull [] data) {
        this.fin = fin;
        this.rsv1 = rsv1;
        this.rsv2 = rsv2;
        this.rsv3 = rsv3;
        this.masked = masked;
        this.opcode = opcode;
        this.data = new ByteBuffer(data);
    }

    /**
     * Constructs a new {@code Frame} by decoding the provided frame header and payload data.
     * This constructor extracts the final frame indicator, reserved bits, and opcode from the
     * frame header, and sets the payload data.
     *
     * @param frame an array of bytes representing the frame header. The first byte should contain
     *              the FIN flag, reserved bits, and opcode.
     * @param data  the payload data of the frame. This array should not be null and contains the
     *              actual message data.
     * @throws IllegalArgumentException if the opcode derived from the frame header is invalid.
     */
    public Frame(byte @NotNull [] frame, byte @NotNull [] data) {
        fin = (frame[0] & 0x80) != 0;
        rsv1 = (frame[0] & 0x40) != 0;
        rsv2 = (frame[0] & 0x20) != 0;
        rsv3 = (frame[0] & 0x10) != 0;
        masked = (frame[1] & 0x80) != 0;

        this.opcode = Opcode.fromByte((byte) (frame[0] & 0x0F));
        this.data = new ByteBuffer(data);
    }

    /**
     * Checks if this frame is the final frame in a sequence of fragmented frames.
     *
     * @return {@code true} if this is the final frame, {@code false} otherwise.
     */
    public boolean isFinalFrame() {
        return fin;
    }

    /**
     * Checks if the RSV1 bit is set in the frame header.
     *
     * @return {@code true} if the RSV1 bit is set, {@code false} otherwise.
     */
    public boolean isRsv1() {
        return rsv1;
    }

    /**
     * Checks if the RSV2 bit is set in the frame header.
     *
     * @return {@code true} if the RSV2 bit is set, {@code false} otherwise.
     */
    public boolean isRsv2() {
        return rsv2;
    }

    /**
     * Checks if the RSV3 bit is set in the frame header.
     *
     * @return {@code true} if the RSV3 bit is set, {@code false} otherwise.
     */
    public boolean isRsv3() {
        return rsv3;
    }

    /**
     * Returns if the frame was masked (when received), or schuld be masked (when send)!
     *
     * @return {@code true} if the frame was masked, {@code false} otherwise.
     */
    public boolean isMasked() {
        return masked;
    }

    /**
     * Sets whether the frame should be masked when sent to the client.
     *
     * @param masked Set to {@code true} if masking should be enabled, otherwise to {@code false}
     */
    public void setMasked(boolean masked) {
        this.masked = masked;
    }

    /**
     * Gets the opcode of this frame which indicates the type of the frame.
     *
     * @return The opcode of this frame.
     */
    public Opcode getOpcode() {
        return opcode;
    }

    /**
     * Sets the {@link Opcode} for the frame.
     *
     * @param opcode The new {@link Opcode}.
     */
    public void setOpcode(@NotNull Opcode opcode) {
        this.opcode = opcode;
    }

    /**
     * Gets the payload data of this frame a {@link ByteBuffer}.
     *
     * @return The payload data as a {@link ByteBuffer}
     */
    public ByteBuffer getBuffer() {
        return data;
    }

    /**
     * Gets the raw payload data of this frame.
     *
     * @return The raw payload data.
     */
    public byte @NotNull [] getData() {
        return data.getSource();
    }

    /**
     * Gets the payload data of this frame as a UTF-8 encoded string.
     *
     * @return The payload data as a UTF-8 string.
     */
    public String getUtf8() {
        return new String(getData(), StandardCharsets.UTF_8);
    }

    /**
     * Sets the payload data for this frame.
     *
     * @param data The payload data to set.
     */
    public void setData(byte[] data) {
        this.data = new ByteBuffer(data);
    }

    /**
     * Appends the data from the specified frame to the current frame. This method is intended to be used
     * for frames with the continuation opcode, allowing fragmented frames to be reassembled into a single
     * frame. The method ensures that only continuation frames are appended and that appending is not attempted
     * on a frame marked as final.
     *
     * <p>This method is synchronized to ensure thread safety during the append operation. It updates the
     * current frame's data by merging it with the data from the specified frame and appropriately sets the
     * final (FIN) flag if either the current frame or the appended frame has the FIN flag set.
     *
     * @param frame the frame whose data is to be appended to the current frame. The frame must have the
     *              continuation opcode and the current frame must not be marked as final.
     * @throws IllegalStateException if the frame's opcode is not continuation or if the current frame
     *                               is already marked as final.
     */
    protected synchronized void appendFrame(Frame frame) {
        if (!frame.getOpcode().equals(Opcode.CONTINUATION))
            throw new IllegalStateException("Tried to append a frame whose opcode is not continuation!");

        if (this.isFinalFrame())
            throw new IllegalStateException("Tried to append a frame to a final frame!");

        byte[] mergedData = new byte[this.getData().length + frame.getData().length];
        System.arraycopy(this.getData(), 0, mergedData, 0, this.getData().length);
        System.arraycopy(frame.getData(), 0, mergedData, this.getData().length, frame.getData().length);
        this.setData(mergedData);

        fin = this.fin || frame.fin;
    }

    /**
     * Fragments the current frame into multiple smaller frames, each with a maximum length specified by the
     * {@code fragmentLength} parameter. This is useful for splitting large frames into smaller fragments
     * to comply with WebSocket protocol requirements for frame sizes.
     *
     * <p>The resulting fragments are stored in a synchronized collection to ensure thread safety. Each
     * fragment will have the same RSV1, RSV2, and RSV3 values as the original frame and a continuation
     * opcode, except for the last fragment which will have its FIN bit set to true to indicate the final fragment.
     *
     * @param fragmentLength the maximum length of each fragmented frame. If the data length is less than
     *                       or equal to {@code fragmentLength}, a single frame is returned.
     * @return a collection of frames, each containing a portion of the original frame's data. The collection
     * is guaranteed to be thread-safe.
     */
    protected synchronized Collection<Frame> fragmentFrame(int fragmentLength) {
        List<Frame> result = new ArrayList<>();
        byte[] data = getData();
        int length = data.length;

        for (int start = 0; start < length; start += fragmentLength) {
            int end = Math.min(length, start + fragmentLength);
            byte[] chunk = new byte[end - start];
            System.arraycopy(data, start, chunk, 0, end - start);
            result.add(new Frame(false, rsv1, rsv2, rsv3, masked, Opcode.CONTINUATION, chunk));
        }

        if (!result.isEmpty()) {
            result.get(result.size() - 1).fin = true;
            result.get(0).opcode = this.opcode;
        }

        return result;
    }

    /**
     * Writes the current frame to the specified output stream. This method encodes the frame header and
     * payload data according to the WebSocket protocol. The method ensures that the frame is correctly
     * formatted with the final frame indicator and the appropriate opcode, followed by the payload length
     * and the actual payload data.
     *
     * @param stream the output stream to which the frame will be written.
     * @throws IOException if an I/O error occurs while writing to the stream.
     */
    protected synchronized void write(OutputStream stream) throws IOException {
        stream.write((byte) ((isFinalFrame() ? 0x80 : 0x00)
                | (isRsv1() ? 0x40 : 0x00)
                | (isRsv2() ? 0x20 : 0x00)
                | (isRsv3() ? 0x10 : 0x00)
                | opcode.byteValue()));

        byte @NotNull [] data = getData();
        int length = data.length;

        byte maskBit = (byte) (isMasked() ? 0x80 : 0x00);
        if (length <= 125)
            stream.write((byte) (maskBit | length));
        else if (length <= 65535) {
            stream.write((byte) (maskBit | 126));
            stream.write((byte) (length >> 8));
            stream.write((byte) length);
        } else {
            stream.write((byte) (maskBit | 127));
            for (int j = 7; j >= 0; j--)
                stream.write((byte) (length >> (8 * j)));
        }


        if (isMasked())
            try {
                byte[] mask = new byte[4];
                SecureRandom.getInstanceStrong().nextBytes(mask);

                // Mask the data
                byte[] masked = new byte[data.length];
                for (int i = 0; i < data.length; i++)
                    masked[i] = (byte) (data[i] ^ mask[i % 4]);

                stream.write(masked);
                return;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

        stream.write(data);
    }

    /**
     * Reads a WebSocket frame from the given input stream.
     * This method reads the frame header and payload data from the provided input stream,
     * handles the payload length based on the WebSocket protocol specifications, and
     * decodes the payload data using the provided masking key.
     *
     * @param stream The input stream to read the frame from.
     * @return The constructed Frame object containing the frame header and payload data.
     * @throws IOException If an I/O error occurs while reading from the stream.
     */
    protected static Frame read(InputStream stream) throws IOException {
        byte[] frame = new byte[10];
        ensureRead(stream, frame, 0, 2);

        byte rawPayloadLength = (byte) (frame[1] & 0x7F);
        if (rawPayloadLength >= 126)
            ensureRead(stream, frame, 2, rawPayloadLength == 126 ? 2 : 8);

        long payloadLength = getPayloadLengthValue(frame, rawPayloadLength);
        if (payloadLength > Integer.MAX_VALUE)
            throw new IOException("Payload size too large to be processed (" + payloadLength + " > " + Integer.MAX_VALUE + ")");

        boolean masked = (frame[1] & 0x80) != 0;
        byte[] masks = new byte[4];
        if (masked)
            ensureRead(stream, masks, 0, masks.length);

        long bytesRead = 0;
        byte[] chunk = new byte[4096];

        try (ByteArrayOutputStream payloadBuilder = new ByteArrayOutputStream((int) payloadLength)) {
            while (bytesRead < payloadLength) {
                int toRead = (int) Math.min(chunk.length, payloadLength - bytesRead);
                int chunkSize = stream.read(chunk, 0, toRead);

                if (chunkSize < 0)
                    throw new IOException("EOF: Failed to read the payload of the frame!");

                // Unmask the data if it was masked
                if (masked)
                    for (int i = 0; i < chunkSize; i++)
                        chunk[i] ^= masks[(int) ((bytesRead + i) % 4)];

                payloadBuilder.write(chunk, 0, chunkSize);
                bytesRead += chunkSize;
            }

            return new Frame(frame, payloadBuilder.toByteArray());
        } finally {
            Arrays.fill(chunk, (byte) 0);
        }
    }

    /**
     * Ensures that the specified number of bytes is read from the given {@link InputStream}.
     * The method repeatedly reads from the stream until the desired amount of bytes is read,
     * or throws an {@link IOException} if the end of the stream is reached prematurely.
     *
     * @param stream the {@link InputStream} to read from.
     * @param buffer the byte array where the data will be stored.
     * @param offset the starting position in the buffer where the data should be written.
     * @param length the number of bytes to read from the stream.
     * @throws IOException if the end of the stream is reached before the expected number of bytes is read,
     *                     or if an I/O error occurs.
     */
    private static void ensureRead(InputStream stream, byte[] buffer, int offset, int length) throws IOException {
        int read = 0;
        while (read < length) {
            int result = stream.read(buffer, offset + read, length - read);
            if (result < 0)
                throw new IOException("EOF: Failed to read the expected amount of bytes (Expected: " + length + ", Actual: " + read + ")");
            read += result;
        }
    }

    /**
     * Extracts the payload length value from the WebSocket frame header.
     *
     * @param frame         The WebSocket frame header.
     * @param payloadLength The payload length value from the frame.
     * @return The actual payload length value.
     */
    private static long getPayloadLengthValue(byte[] frame, byte payloadLength) {
        if (payloadLength == 126)
            // If the payload length is 126, combine the 3rd and 4th bytes to get the actual length.
            return ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
        else if (payloadLength == 127)
            // If the payload length is 127, combine the 3rd to 10th bytes to get the actual length.
            return ((frame[2] & 0xFFL) << 56)
                    | ((frame[3] & 0xFFL) << 48)
                    | ((frame[4] & 0xFFL) << 40)
                    | ((frame[5] & 0xFFL) << 32)
                    | ((frame[6] & 0xFFL) << 24)
                    | ((frame[7] & 0xFFL) << 16)
                    | ((frame[8] & 0xFFL) << 8)
                    | (frame[9] & 0xFFL);
        else
            // For payload lengths less than 126, the payloadLength itself represents the actual length.
            return payloadLength;
    }

    /**
     * Creates a deep copy of this frame.
     *
     * @return The deep copy.
     */
    @Override
    protected Object clone() {
        return new Frame(fin, rsv1, rsv2, rsv3, masked, opcode, data.getSource().clone());
    }

}
