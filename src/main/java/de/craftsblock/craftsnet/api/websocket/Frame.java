package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.craftsblock.craftsnet.api.requirements.RequireAble;
import de.craftsblock.craftsnet.api.websocket.exception.FrameReadException;
import de.craftsblock.craftsnet.api.websocket.exception.MaxFrameSizeExceededException;
import de.craftsblock.craftsnet.api.websocket.exception.UnmaskedFrameException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 3.0.6-SNAPSHOT
 */
public class Frame implements RequireAble {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private Opcode opcode;
    private byte[] data;
    private BufferUtil bufferUtil;

    private boolean fin;
    private final boolean rsv1, rsv2, rsv3;
    private boolean masked;

    private List<byte[]> fragments;
    private int fragmentedMessageSize;

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
        this.setData(data);
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
        this(
                (frame[0] & 0x80) != 0,
                (frame[0] & 0x40) != 0,
                (frame[0] & 0x20) != 0,
                (frame[0] & 0x10) != 0,
                (frame[1] & 0x80) != 0,
                Opcode.fromByte((byte) (frame[0] & 0x0F)),
                data
        );
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
     * Gets the raw payload data of this frame.
     *
     * @return The raw payload data.
     */
    public byte @NotNull [] getData() {
        return data;
    }

    /**
     * Gets the payload data of this frame a {@link BufferUtil}.
     *
     * @return The payload data as a {@link BufferUtil}
     * @since 3.7.0
     */
    public BufferUtil getBufferUtil() {
        if (this.bufferUtil == null) {
            this.bufferUtil = BufferUtil.wrap(data);
        }

        return this.bufferUtil;
    }

    /**
     * Gets the payload data of this frame a {@link ByteBuffer}.
     *
     * @return The payload data as a {@link ByteBuffer}
     * @since 3.7.0
     */
    public ByteBuffer getByteBuffer() {
        return this.getBufferUtil().getRaw();
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
        this.data = data;
        this.bufferUtil = null;
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
     * @param frame          the frame whose data is to be appended to the current frame. The frame must have the
     *                       continuation opcode and the current frame must not be marked as final.
     * @param maxMessageSize the maximum length of the total message in bytes.
     * @throws IllegalStateException if the frame's opcode is not continuation or if the current frame
     *                               is already marked as final.
     */
    protected synchronized void appendFrame(Frame frame, int maxMessageSize) throws FrameReadException {
        if (frame == null) {
            throw new IllegalArgumentException("Frame must not be null!");
        }

        if (frame.getOpcode() != Opcode.CONTINUATION) {
            throw new FrameReadException(
                    ClosureCode.PROTOCOL_ERROR,
                    "Can not append a non continuation frame"
            );
        }

        if (this.isFinalFrame()) {
            throw new FrameReadException(
                    ClosureCode.PROTOCOL_ERROR,
                    "Tried to append a frame to a final frame"
            );
        }

        if (frame.rsv1 || frame.rsv2 || frame.rsv3) {
            throw new FrameReadException(
                    ClosureCode.PROTOCOL_ERROR,
                    "Continuation frames must not set RSV bits"
            );
        }

        if (this.fragments == null) {
            this.fragments = new ArrayList<>();
            this.fragments.add(this.data);
            this.fragmentedMessageSize = this.data.length;
        }

        byte[] frameData = frame.data;
        long mergedLength = this.fragmentedMessageSize + frameData.length;

        if (mergedLength > maxMessageSize) {
            throw new MaxFrameSizeExceededException();
        }

        this.fragments.add(frameData);
        this.fragmentedMessageSize = (int) mergedLength;
        this.fin = frame.fin;

        if (this.fin) {
            byte[] mergedData = new byte[this.fragmentedMessageSize];
            int offset = 0;

            for (byte[] fragment : this.fragments) {
                System.arraycopy(fragment, 0, mergedData, offset, fragment.length);
                offset += fragment.length;
            }

            this.setData(mergedData);
            this.fragments = null;
            this.fragmentedMessageSize = 0;
        }
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
        if (fragmentLength <= 0) {
            throw new IllegalArgumentException(
                    "fragmentLength must be greater than 0"
            );
        }

        byte[] data = getData();
        if (data.length <= fragmentLength) {
            return List.of(this);
        }

        List<Frame> result = new ArrayList<>();

        for (int start = 0; start < data.length; start += fragmentLength) {
            int end = Math.min(data.length, start + fragmentLength);

            byte[] chunk = Arrays.copyOfRange(data, start, end);

            boolean first = start == 0;
            boolean last = end == data.length;

            result.add(new Frame(
                    last,
                    first && rsv1,
                    first && rsv2,
                    first && rsv3,
                    masked,
                    first ? opcode : Opcode.CONTINUATION,
                    chunk
            ));
        }

        return result;
    }

    /**
     * Writes the current frame to the specified output stream. This method encodes
     * the frame header and payload data according to the WebSocket protocol.
     *
     * <p>The complete frame, including its header, optional masking key and
     * payload, is assembled into a single byte array and written to the stream
     * using one write operation.
     *
     * @param stream the output stream to which the frame will be written.
     * @throws IOException if an I/O error occurs while writing to the stream.
     */
    protected synchronized void write(OutputStream stream) throws IOException {
        byte[] data = getData();
        int length = data.length;

        int headerLength = length <= 125 ? 2 : length <= 65535 ? 4 : 10;
        int maskLength = isMasked() ? 4 : 0;

        byte[] output = new byte[headerLength + maskLength + length];
        output[0] = (byte) (
                (isFinalFrame() ? 0x80 : 0x00)
                        | (isRsv1() ? 0x40 : 0x00)
                        | (isRsv2() ? 0x20 : 0x00)
                        | (isRsv3() ? 0x10 : 0x00)
                        | opcode.byteValue()
        );

        byte maskBit = (byte) (isMasked() ? 0x80 : 0x00);
        if (length <= 125) {
            output[1] = (byte) (maskBit | length);
        } else if (length <= 65535) {
            output[1] = (byte) (maskBit | 126);
            output[2] = (byte) (length >>> 8);
            output[3] = (byte) length;
        } else {
            output[1] = (byte) (maskBit | 127);

            for (int i = 0; i < 8; i++) {
                output[2 + i] = (byte) ((long) length >>> (56 - i * 8));
            }
        }

        int payloadOffset = headerLength;
        if (isMasked()) {
            byte[] mask = new byte[4];
            SECURE_RANDOM.nextBytes(mask);

            System.arraycopy(mask, 0, output, headerLength, maskLength);
            payloadOffset += maskLength;

            for (int i = 0; i < length; i++) {
                output[payloadOffset + i] = (byte) (data[i] ^ mask[i % 4]);
            }
        } else {
            System.arraycopy(data, 0, output, payloadOffset, length);
        }

        stream.write(output);
    }

    /**
     * Reads a WebSocket frame from the given input stream.
     * This method reads the frame header and payload data from the provided input stream,
     * handles the payload length based on the WebSocket protocol specifications, and
     * decodes the payload data using the provided masking key.
     *
     * @param stream The input stream to read the frame from.
     * @return The constructed Frame object containing the frame header and payload data.
     * @throws FrameReadException If the read fails in a way the client should know.
     * @throws IOException        If an I/O error occurs while reading from the stream.
     */
    protected static Frame read(InputStream stream, int maxFrameSize) throws FrameReadException, IOException {
        byte[] frame = new byte[10];
        ensureRead(stream, frame, 0, 2);

        int rawPayloadLength = frame[1] & 0x7F;
        if (rawPayloadLength >= 126) {
            ensureRead(stream, frame, 2, rawPayloadLength == 126 ? 2 : 8);
        }

        long payloadLength = getPayloadLengthValue(frame, rawPayloadLength);
        if (payloadLength > maxFrameSize) {
            throw new MaxFrameSizeExceededException();
        }

        boolean masked = (frame[1] & 0x80) != 0;
        if (!masked) {
            throw new UnmaskedFrameException();
        }

        byte[] mask = new byte[4];
        ensureRead(stream, mask, 0, mask.length);

        byte[] payload = new byte[(int) payloadLength];
        ensureRead(stream, payload, 0, payload.length);

        for (int i = 0; i < payload.length; i++) {
            payload[i] ^= mask[i & 3];
        }

        frame[1] &= 0x7F;

        return new Frame(frame, payload);
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
    private static int ensureRead(InputStream stream, byte[] buffer, int offset, int length) throws IOException {
        int read = 0;

        while (read < length) {
            int result = stream.read(buffer, offset + read, length - read);
            if (result < 0) {
                throw new IOException(
                        "EOF: Can not read the expected amount " + length + " of bytes"
                );
            }

            if (result == 0) {
                int singleByte = stream.read();

                if (singleByte < 0) {
                    throw new IOException(
                            "EOF: Can not read the expected amount " + length + " of bytes"
                    );
                }

                buffer[offset + read] = (byte) singleByte;
                read++;
                continue;
            }

            read += result;
        }

        return read;
    }

    /**
     * Decodes the payload length from a WebSocket frame header.
     *
     * <p>The payload length is encoded using one of three formats:
     * <ul>
     *     <li>Values from 0 to 125 are encoded directly.</li>
     *     <li>Value 126 indicates a 16-bit extended payload length.</li>
     *     <li>Value 127 indicates a 64-bit extended payload length.</li>
     * </ul>
     *
     * <p>This method also validates the payload length encoding according
     * to RFC 6455, ensuring that extended payload lengths use the minimum
     * required encoding.
     *
     * @param frame         the frame header containing the encoded payload length;
     *                      must contain at least 2, 4 or 10 bytes depending on the encoding
     * @param payloadLength the raw 7-bit payload length indicator from the
     *                      second frame header byte
     * @return the decoded payload length as a non-negative long value
     * @throws FrameReadException if the payload length encoding is invalid,
     *                            non-minimal or violates RFC 6455
     */
    private static long getPayloadLengthValue(byte[] frame, int payloadLength) throws FrameReadException {
        return switch (payloadLength) {
            case 126 -> {
                long length = ((frame[2] & 0xFFL) << 8)
                        | (frame[3] & 0xFFL);

                if (length < 126) {
                    throw new FrameReadException(
                            ClosureCode.PROTOCOL_ERROR,
                            "Non-minimal payload length encoding!"
                    );
                }

                yield length;
            }

            case 127 -> {
                if ((frame[2] & 0x80) != 0) {
                    throw new FrameReadException(
                            ClosureCode.PROTOCOL_ERROR,
                            "Most significant bit must be 0!"
                    );
                }

                yield getLengthLong(frame);
            }

            default -> {
                if (payloadLength < 0 || payloadLength > 125) {
                    throw new FrameReadException(
                            ClosureCode.PROTOCOL_ERROR,
                            "Invalid payload length encoding!"
                    );
                }

                yield payloadLength;
            }
        };
    }

    /**
     * Decodes a 64-bit extended payload length from a WebSocket frame header.
     *
     * <p>The length is encoded as an unsigned 64-bit integer in network byte
     * order (big-endian). Since Java does not provide an unsigned long type,
     * the most significant bit must be zero to ensure that the decoded value
     * can be represented by a positive signed long.
     *
     * <p>This method also ensures that the encoded length is at least 65536,
     * as required by the WebSocket protocol's minimal encoding rules.
     *
     * @param frame the frame header containing the 64-bit extended payload
     *              length; must contain at least 10 bytes
     * @return the decoded payload length as a non-negative long value
     * @throws FrameReadException if the encoded length is smaller than
     *                            65536 or cannot be represented by a
     *                            positive signed long
     */
    private static long getLengthLong(byte[] frame) throws FrameReadException {
        long length = ((frame[2] & 0xFFL) << 56)
                | ((frame[3] & 0xFFL) << 48)
                | ((frame[4] & 0xFFL) << 40)
                | ((frame[5] & 0xFFL) << 32)
                | ((frame[6] & 0xFFL) << 24)
                | ((frame[7] & 0xFFL) << 16)
                | ((frame[8] & 0xFFL) << 8)
                | (frame[9] & 0xFFL);

        if (length < 65536) {
            throw new FrameReadException(
                    ClosureCode.PROTOCOL_ERROR,
                    "Non-minimal payload length encoding!"
            );
        }

        return length;
    }

    /**
     * Creates a deep copy of this frame.
     *
     * @return The deep copy.
     */
    @Override
    protected Object clone() {
        return new Frame(fin, rsv1, rsv2, rsv3, masked, opcode, data.clone());
    }

}
