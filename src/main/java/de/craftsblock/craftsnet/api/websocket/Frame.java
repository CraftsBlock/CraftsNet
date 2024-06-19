package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftsnet.api.requirements.RequireAble;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
 * @version 1.0.0
 * @since 3.0.6-SNAPSHOT
 */
public class Frame implements RequireAble {

    private final ControlByte opcode;
    private byte[] data;

    private boolean fin;
    private final boolean rsv1;
    private final boolean rsv2;
    private final boolean rsv3;

    /**
     * Constructs a Frame with the given frame header and data.
     *
     * @param frame The frame header.
     * @param data  The payload data.
     */
    public Frame(byte[] frame, byte[] data) {
        fin = (frame[0] & 0x80) != 0;
        rsv1 = (frame[0] & 0x40) != 0;
        rsv2 = (frame[0] & 0x20) != 0;
        rsv3 = (frame[0] & 0x10) != 0;

        this.opcode = ControlByte.fromByte((byte) (frame[0] & 0x0F));
        this.data = data;
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
     * Gets the opcode of this frame which indicates the type of the frame.
     *
     * @return The opcode of this frame.
     */
    public ControlByte getOpcode() {
        return opcode;
    }

    /**
     * Gets the payload data of this frame as a UTF-8 encoded string.
     *
     * @return The payload data as a UTF-8 string.
     */
    public String getUtf8() {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Gets the raw payload data of this frame.
     *
     * @return The raw payload data.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Sets the payload data for this frame.
     *
     * @param data The payload data to set.
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Appends another frame's data to this frame.
     * This method is used to handle fragmented frames by combining their data.
     * The method checks if the provided frame has a continuation opcode and if this frame
     * is not already marked as the final frame. If these conditions are met, it merges
     * the data from the provided frame into this frame.
     *
     * @param frame The frame to append. This frame should have the continuation opcode.
     * @throws IllegalStateException if the frame's opcode is not a continuation or if this frame is already the final frame.
     */
    protected synchronized void appendFrame(Frame frame) {
        if (!frame.getOpcode().equals(ControlByte.CONTINUATION))
            throw new IllegalStateException("Tried to append a frame whose opcode is not a continuation!");

        if (this.isFinalFrame())
            throw new IllegalStateException("Tried to append a frame while the current frame was already the last one!");

        byte[] mergedData = new byte[this.getData().length + frame.getData().length];
        System.arraycopy(this.getData(), 0, mergedData, 0, this.getData().length);
        System.arraycopy(frame.getData(), 0, mergedData, this.getData().length, frame.getData().length);
        this.setData(mergedData);

        fin = this.fin || frame.fin;
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
        stream.read(frame, 0, 2);

        byte payloadLength = (byte) (frame[1] & 0x7F);
        if (payloadLength == 126) stream.read(frame, 2, 2);
        else if (payloadLength == 127) stream.read(frame, 2, 8);

        long payloadLengthValue = getPayloadLengthValue(frame, payloadLength);

        byte[] masks = new byte[4];
        stream.read(masks);

        ByteArrayOutputStream payloadBuilder = new ByteArrayOutputStream();

        long bytesRead = 0;
        long bytesToRead = payloadLengthValue;

        byte[] chunk = new byte[4096];
        int chunkSize;

        while (bytesToRead > 0 && (chunkSize = stream.read(chunk, 0, (int) Math.min(chunk.length, bytesToRead))) != -1) {
            for (int i = 0; i < chunkSize; i++)
                chunk[i] ^= masks[(int) ((bytesRead + i) % 4)];
            payloadBuilder.write(chunk, 0, chunkSize);

            bytesRead += chunkSize;
            bytesToRead -= chunkSize;
        }

        return new Frame(frame, payloadBuilder.toByteArray());
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

}
