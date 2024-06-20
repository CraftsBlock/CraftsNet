package de.craftsblock.craftsnet.api.websocket;

import java.util.List;

/**
 * Enumeration representing WebSocket opcodes along with their integer values.
 * These opcodes are defined in the WebSocket protocol.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.5-SNAPSHOT
 */
public enum Opcode {

    /**
     * Indicates a continuation frame.
     */
    CONTINUATION(0x00),

    /**
     * Indicates a text frame.
     */
    TEXT(0x01),

    /**
     * Indicates a binary frame.
     */
    BINARY(0x02),

    /**
     * Indicates a Ping frame.
     */
    PING(0x09),

    /**
     * Indicates a Pong frame.
     */
    PONG(0x0A),

    /**
     * Indicates a Close frame.
     */
    CLOSE(0x08);

    private static final List<Opcode> DATA_CODES = List.of(TEXT, BINARY, CONTINUATION);
    private static final List<Opcode> CONTROL_CODES = List.of(CLOSE, PING, PONG);

    private final int intValue;

    /**
     * Constructs an Opcode with the specified integer value.
     *
     * @param intValue The int value representing the opcode.
     */
    Opcode(int intValue) {
        this.intValue = intValue;
    }

    /**
     * Retrieves the integer value of the opcode.
     *
     * @return The integer value representing the opcode.
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Retrieves the byte value of the opcode.
     *
     * @return The byte value representing the opcode.
     */
    public byte byteValue() {
        return (byte) intValue;
    }

    /**
     * Checks if the opcode is a code which should contain data
     *
     * @return true if is a data code, false otherwise
     */
    public boolean isDataCode() {
        return DATA_CODES.contains(this);
    }

    /**
     * Checks if the opcode is a code which should contain control instruction
     *
     * @return true if is a control code, false otherwise
     */
    public boolean isControlCode() {
        return CONTROL_CODES.contains(this);
    }

    /**
     * Retrieves the opcode corresponding to the given integer value.
     *
     * @param controlByte The integer value of the opcode.
     * @return The ControlByte corresponding to the integer value, or null if not found.
     */
    public static Opcode fromInt(int controlByte) {
        for (Opcode b : Opcode.values())
            if ((b.byteValue() & 0x0F) == (controlByte & 0x0F))
                return b;
        return null;
    }

    /**
     * Retrieves the opcode corresponding to the given byte value.
     *
     * @param controlByte The byte value of the opcode.
     * @return The ControlByte corresponding to the byte value, or null if not found.
     */
    public static Opcode fromByte(byte controlByte) {
        return fromInt(controlByte);
    }

}
