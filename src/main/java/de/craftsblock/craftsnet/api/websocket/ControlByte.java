package de.craftsblock.craftsnet.api.websocket;

/**
 * Enumeration representing WebSocket control bytes along with their integer values.
 * These control bytes are defined in the WebSocket protocol.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.5-SNAPSHOT
 */
public enum ControlByte {

    /**
     * Indicates a text frame.
     */
    TEXT(0xFFFFFF81),

    /**
     * Indicates a binary frame.
     */
    BINARY(0xFFFFFF82),

    /**
     * Indicates a Ping frame.
     */
    PING(0xFFFFFF89),

    /**
     * Indicates a Pong frame.
     */
    PONG(0xFFFFFF8A),

    /**
     * Indicates a Close frame.
     */
    CLOSE(0xFFFFFF88);

    private final int controlByte;

    /**
     * Constructs a ControlByte with the specified integer value.
     *
     * @param controlByte The integer value representing the control byte.
     */
    ControlByte(int controlByte) {
        this.controlByte = controlByte;
    }

    /**
     * Retrieves the integer value of the ControlByte.
     *
     * @return The integer value representing the control byte.
     */
    public int intValue() {
        return controlByte;
    }

    /**
     * Retrieves the byte value of the ControlByte.
     *
     * @return The byte value representing the control byte.
     */
    public byte byteValue() {
        return (byte) controlByte;
    }

    /**
     * Retrieves the ControlByte corresponding to the given integer value.
     *
     * @param controlByte The integer value of the control byte.
     * @return The ControlByte corresponding to the integer value, or null if not found.
     */
    public static ControlByte fromInt(int controlByte) {
        for (ControlByte b : ControlByte.values())
            if (b.intValue() == controlByte)
                return b;
        return null;
    }

    /**
     * Retrieves the ControlByte corresponding to the given byte value.
     *
     * @param controlByte The byte value of the control byte.
     * @return The ControlByte corresponding to the byte value, or null if not found.
     */
    public static ControlByte fromByte(byte controlByte) {
        return fromInt(controlByte);
    }

}
