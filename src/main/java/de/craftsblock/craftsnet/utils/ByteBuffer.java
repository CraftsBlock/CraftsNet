package de.craftsblock.craftsnet.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * A custom {@code ByteBuffer} implementation that provides both read and write operations
 * for byte arrays. It supports features such as fixed-size buffers, read-only buffers,
 * and the ability to read/write various data types including integers, floating point values,
 * strings, and {@link UUID}s.
 * <p>
 * This class also allows the marking and resetting of read and write positions,
 * and can handle both little-endian and big-endian data representation through shifted reads and writes.
 * <p>
 * Implements {@link AutoCloseable} to support automatic resource management.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.1.0-SNAPSHOT
 */
public class ByteBuffer {

    private final boolean readOnly, fixedSize;

    private byte[] source;
    private int readerIndex, rememberedReaderIndex, writerIndex, rememberedWriterIndex;

    /**
     * Constructs a new {@code ByteBuffer} with the given initial size.
     * The buffer is both read-only and fixed in size by default.
     *
     * @param initialSize the initial size of the buffer.
     */
    public ByteBuffer(int initialSize) {
        this(initialSize, true);
    }

    /**
     * Constructs a new {@code ByteBuffer} with the given initial size and the option to set a fixed size.
     *
     * @param initialSize the initial size of the buffer.
     * @param fixedSize   whether the buffer size is fixed or can be expanded.
     */
    public ByteBuffer(int initialSize, boolean fixedSize) {
        this(initialSize, false, fixedSize);
    }

    /**
     * Constructs a new {@code ByteBuffer} with the given initial size, read-only, and fixed-size flags.
     *
     * @param initialSize the initial size of the buffer.
     * @param readOnly    whether the buffer is read-only.
     * @param fixedSize   whether the buffer size is fixed or can be expanded.
     */
    public ByteBuffer(int initialSize, boolean readOnly, boolean fixedSize) {
        this(new byte[initialSize], readOnly, fixedSize);
        writerIndex(0);
        markWriterIndex();
    }

    /**
     * Constructs a new {@code ByteBuffer} from an existing byte array.
     * The buffer is read-only and fixed in size by default.
     *
     * @param source the byte array to initialize the buffer with.
     */
    public ByteBuffer(byte[] source) {
        this(source, true);
    }

    /**
     * Constructs a new {@code ByteBuffer} from an existing byte array with the option to set the buffer as read-only.
     *
     * @param source   the byte array to initialize the buffer with.
     * @param readOnly whether the buffer is read-only.
     */
    public ByteBuffer(byte[] source, boolean readOnly) {
        this(source, readOnly, true);
    }

    /**
     * Constructs a new {@code ByteBuffer} from an existing byte array with options for read-only and fixed size.
     *
     * @param source    the byte array to initialize the buffer with.
     * @param readOnly  whether the buffer is read-only.
     * @param fixedSize whether the buffer size is fixed or can be expanded.
     */
    public ByteBuffer(byte[] source, boolean readOnly, boolean fixedSize) {
        this.source = source;
        this.readOnly = readOnly;
        this.fixedSize = fixedSize;
        this.readerIndex = this.rememberedReaderIndex = 0;
        this.writerIndex = this.rememberedWriterIndex = this.source.length;
    }

    /**
     * Reads the specified number of bytes from the buffer and advances the reader index.
     *
     * @param length the number of bytes to read.
     * @return the bytes read as a new byte array.
     */
    public byte[] readNBytes(int length) {
        byte[] read = Arrays.copyOfRange(source, readerIndex, readerIndex + length);
        this.readerIndex += length;
        return read;
    }

    /**
     * Reads all remaining bytes from the current reader index.
     *
     * @return the remaining bytes in the buffer.
     */
    public byte[] readRemaining() {
        return readNBytes(size() - readerIndex);
    }

    /**
     * Reads a single byte from the buffer.
     *
     * @return the byte that was read.
     */
    public byte readByte() {
        return readNBytes(1)[0];
    }

    /**
     * Reads a shifted integer from the buffer by combining bytes.
     * This method reads {@code length} bytes, shifts them according to the given shift value, and
     * assembles the result.
     *
     * @param length the number of bytes to read.
     * @param shift  the initial shift amount.
     * @return the assembled integer value.
     */
    public int readShifted(int length, int shift) {
        byte[] read = readNBytes(length);
        int result = 0;
        for (int i = 0; i < length; i++) {
            result |= (read[i] & 0xFF) << shift;
            shift -= 8;
        }
        return result;
    }

    /**
     * Reads a short (2 bytes) from the buffer.
     *
     * @return the short value read from the buffer.
     */
    public short readShort() {
        return (short) readShifted(2, 8);
    }

    /**
     * Reads an unsigned short (2 bytes) from the buffer.
     *
     * @return the unsigned short value as an integer.
     */
    public int readUnsignedShort() {
        return Short.toUnsignedInt(readShort());
    }

    /**
     * Reads an integer (4 bytes) from the buffer.
     *
     * @return the integer value read from the buffer.
     */
    public int readInt() {
        return readShifted(4, 24);
    }

    /**
     * Reads a variable-length integer from the buffer.
     * This method reads the VarInt format, where the integer is encoded
     * in one to five bytes. If the VarInt exceeds 5 bytes, a {@link RuntimeException} is thrown.
     *
     * @return the decoded VarInt.
     * @throws RuntimeException if the VarInt is too large (more than 5 bytes).
     */
    public int readVarInt() {
        int numRead = 0;
        int result = 0;

        byte read;
        do {
            read = readByte();
            int value = (read & 127);
            result |= (value << (7 * numRead));

            if (++numRead > 5)
                throw new RuntimeException("VarInt is too large");
        } while ((read & 128) != 0);

        return result;
    }

    /**
     * Reads a floating-point value (4 bytes) from the buffer.
     * The float is read as its raw integer bits, which are then converted
     * back to a float using {@link Float#intBitsToFloat(int)}.
     *
     * @return the float value read from the buffer.
     */
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads a long (8 bytes) from the buffer.
     * The long is constructed by combining two integer values.
     *
     * @return the long value read from the buffer.
     */
    public long readLong() {
        return ((long) readInt()) << 32 | (readInt() & 0xFFFFFFFFL);
    }

    /**
     * Reads a variable-length long from the buffer.
     * This method reads the VarLong format, where the long is encoded
     * in one to ten bytes. If the VarLong exceeds 10 bytes, a {@link RuntimeException} is thrown.
     *
     * @return the decoded VarLong.
     * @throws RuntimeException if the VarLong is too large (more than 10 bytes).
     */
    public long readVarLong() {
        int numRead = 0;
        long result = 0;

        byte read;
        do {
            read = readByte();
            long value = (read & 127);
            result |= (value << (7 * numRead));

            if (++numRead > 10)
                throw new RuntimeException("VarLong is too large");
        } while ((read & 128) != 0);

        return result;
    }

    /**
     * Reads a double value (8 bytes) from the buffer.
     * The double is read as its raw long bits, which are then converted
     * back to a double using {@link Double#longBitsToDouble(long)}.
     *
     * @return the double value read from the buffer.
     */
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads a boolean value from the buffer.
     * This method reads a single byte and returns {@code true} if the byte is non-zero,
     * or {@code false} if the byte is zero.
     *
     * @return the boolean value read from the buffer.
     */
    public boolean readBoolean() {
        return readByte() != 0;
    }

    /**
     * Reads a single character from the buffer.
     * This method reads a byte and casts it to a {@code char}.
     *
     * @return the character read from the buffer.
     */
    public char readChar() {
        return (char) readByte();
    }

    /**
     * Reads a UTF-8 encoded string from the buffer.
     * This method first reads the length of the string (stored as a VarInt),
     * followed by the actual UTF-8 encoded bytes of the string.
     *
     * @return the UTF string read from the buffer.
     */
    public String readUTF() {
        return readUTF(readVarInt());
    }

    /**
     * Reads a UTF-8 encoded string of the given length from the buffer.
     *
     * @param length the length of the UTF string to read.
     * @return the UTF string read from the buffer.
     */
    public String readUTF(int length) {
        return readCharSequence(length, StandardCharsets.UTF_8);
    }

    /**
     * Reads a sequence of characters from the buffer using the specified {@link Charset}.
     * This method reads the specified number of bytes from the buffer and decodes
     * them as a character sequence in the provided charset.
     *
     * @param length  the number of bytes to read.
     * @param charset the {@link Charset} to use for decoding.
     * @return the decoded character sequence.
     */
    public String readCharSequence(int length, Charset charset) {
        return new String(readNBytes(length), charset);
    }

    /**
     * Reads an enum constant from the buffer by ordinal value.
     * The method reads an integer from the buffer, then returns the corresponding
     * enum constant for the provided {@code type}.
     *
     * @param <T>  the enum type.
     * @param type the class of the enum type.
     * @return the enum constant corresponding to the ordinal value.
     */
    public <T extends Enum<?>> T readEnum(Class<T> type) {
        return type.getEnumConstants()[readInt()];
    }

    /**
     * Reads a {@link UUID} from the buffer, which consists of two long values
     * (most significant bits and least significant bits).
     *
     * @return the {@link UUID} read from the buffer.
     */
    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    /**
     * Returns the current reader index of the buffer.
     *
     * @return the reader index.
     */
    public int readerIndex() {
        return readerIndex;
    }

    /**
     * Sets the reader index to the specified value.
     *
     * @param readerIndex the new reader index.
     */
    public void readerIndex(int readerIndex) {
        this.readerIndex = readerIndex;
    }

    /**
     * Marks the current reader index so it can be restored later.
     */
    public void markReaderIndex() {
        this.rememberedReaderIndex = this.readerIndex;
    }

    /**
     * Resets the reader index to the previously marked index.
     */
    public void resetReaderIndex() {
        this.readerIndex = this.rememberedReaderIndex;
    }

    /**
     * Returns the number of readable bytes left in the buffer.
     * This is the number of bytes between the current reader index and the end of the buffer.
     *
     * @return the number of readable bytes.
     */
    public int readableBytes() {
        return this.source.length - this.readerIndex;
    }

    /**
     * Checks if there are any readable bytes remaining in the buffer.
     *
     * @return {@code true} if there are readable bytes remaining, otherwise {@code false}.
     */
    public boolean isReadable() {
        return isReadable(0);
    }

    /**
     * Checks if the buffer has enough readable bytes for a specified length.
     *
     * @param length the number of bytes to check for.
     * @return {@code true} if the buffer has at least {@code length} readable bytes, otherwise {@code false}.
     */
    public boolean isReadable(int length) {
        return this.writerIndex - (this.readerIndex + length) > 0;
    }

    /**
     * Writes the specified byte array to the buffer at the current writer index.
     *
     * @param data the byte array to write.
     */
    public void write(byte[] data) {
        if (isReadOnly()) throw new IllegalStateException("The byte buffer was marked as read only!");
        int length = data.length;
        if (!isWriteable(length))
            throw new ArrayIndexOutOfBoundsException("The length " + (length + writerIndex) + " is out of range! (Fixed length: " + source.length + ")");

        byte[] array;
        if (!isFixedSize() && length + writerIndex >= source.length) {
            array = new byte[length + writerIndex];
            System.arraycopy(this.source, 0, array, 0, this.source.length);
        } else array = this.source;

        System.arraycopy(data, 0, array, writerIndex, length);
        this.writerIndex += data.length;
        this.source = array;
    }

    /**
     * Writes a single byte to the buffer.
     *
     * @param b the byte to write.
     */
    public void writeByte(byte b) {
        write(new byte[]{b});
    }

    /**
     * Writes a single byte (represented as an integer) to the buffer.
     * This method casts the integer to a byte before writing.
     *
     * @param b the byte to write, as an integer.
     */
    public void writeByte(int b) {
        write(new byte[]{(byte) b});
    }

    /**
     * Writes a value to the buffer, shifting the bits as specified.
     * This method writes the value byte by byte, shifting the bits
     * by 8 for each subsequent byte.
     *
     * @param value the value to write.
     * @param shift the number of bits to shift for the first byte.
     */
    private void writeShifted(long value, int shift) {
        for (int i = shift; i >= 0; i -= 8)
            writeByte((byte) (value >> i));
    }

    /**
     * Writes a short (2 bytes) to the buffer.
     * The value is written with a shift of 8 bits.
     *
     * @param value the short value to write.
     */
    public void writeShort(short value) {
        writeShifted(value, 8);
    }

    /**
     * Writes an unsigned short (2 bytes) to the buffer.
     * The value is written with a shift of 8 bits.
     *
     * @param value the unsigned short value to write.
     */
    public void writeUnsignedShort(int value) {
        writeShifted(value, 8);
    }

    /**
     * Writes a variable-length integer to the buffer using the VarInt format.
     * The integer is encoded into one to five bytes. This method writes one byte at a time
     * until all significant bits are encoded.
     *
     * @param value the VarInt to write.
     */
    public void writeVarInt(int value) {
        while ((value & -128) != 0) {
            this.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        this.writeByte(value);
    }

    /**
     * Writes an integer (4 bytes) to the buffer.
     * The value is written with a shift of 24 bits.
     *
     * @param value the integer value to write.
     */
    public void writeInt(int value) {
        writeShifted(value, 24);
    }

    /**
     * Writes a floating-point value (4 bytes) to the buffer.
     * The float is first converted to its raw integer bits, then written to the buffer.
     *
     * @param value the float value to write.
     */
    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    /**
     * Writes a long (8 bytes) to the buffer.
     * The value is written with a shift of 56 bits.
     *
     * @param value the long value to write.
     */
    public void writeLong(long value) {
        writeShifted(value, 56);
    }

    /**
     * Writes a variable-length long to the buffer using the VarLong format.
     * The long is encoded into one to ten bytes. This method writes one byte at a time
     * until all significant bits are encoded.
     *
     * @param value the VarLong to write.
     */
    public void writeVarLong(long value) {
        while ((value & -128) != 0) {
            this.writeByte((int) (value & 127L) | 128);
            value >>>= 7;
        }
        this.writeByte((int) value);
    }

    /**
     * Writes a double value (8 bytes) to the buffer.
     * The double is first converted to its raw long bits, then written to the buffer.
     *
     * @param value the double value to write.
     */
    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    /**
     * Writes a boolean value to the buffer.
     * If the value is {@code true}, it writes a byte with value {@code 1};
     * if {@code false}, it writes a byte with value {@code 0}.
     *
     * @param value the boolean value to write.
     */
    public void writeBoolean(boolean value) {
        writeByte((byte) (value ? 1 : 0));
    }

    /**
     * Writes a single character to the buffer.
     * This method writes the character as a single byte.
     *
     * @param value the character to write.
     */
    public void writeChar(char value) {
        writeByte((byte) value);
    }

    /**
     * Writes a UTF-8 encoded string to the buffer.
     * This method first writes the length of the UTF string using {@code writeVarInt()},
     * followed by the UTF-8 encoded bytes of the string.
     *
     * @param value the UTF string to write.
     */
    public void writeUTF(String value) {
        writeVarInt(value.getBytes(StandardCharsets.UTF_8).length);
        writeCharSequence(value, StandardCharsets.UTF_8);
    }

    /**
     * Writes a {@link CharSequence} to the buffer using the specified {@link Charset}.
     * The char sequence is converted into a byte array using the given character encoding,
     * and the byte array is then written to the buffer.
     *
     * @param value   the {@code CharSequence} to write.
     * @param charset the {@code Charset} to use for encoding.
     */
    public void writeCharSequence(CharSequence value, Charset charset) {
        write(value.toString().getBytes(charset));
    }

    /**
     * Writes an enum constant to the buffer by its ordinal value.
     * This method writes the ordinal (position in enum declaration) of the given enum constant.
     *
     * @param <T> the enum type.
     * @param t   the enum constant to write.
     */
    public <T extends Enum<?>> void writeEnum(T t) {
        writeInt(t.ordinal());
    }

    /**
     * Writes a {@link UUID} to the buffer. A UUID consists of two long values:
     * the most significant bits and the least significant bits.
     *
     * @param uuid the {@link UUID} to write.
     */
    public void writeUUID(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    /**
     * Returns the current writer index of the buffer.
     *
     * @return the writer index.
     */
    public int writerIndex() {
        return writerIndex;
    }

    /**
     * Sets the writer index to the specified value.
     *
     * @param writerIndex the new writer index.
     */
    public void writerIndex(int writerIndex) {
        this.writerIndex = writerIndex;
    }

    /**
     * Marks the current writer index, so it can be restored later.
     */
    public void markWriterIndex() {
        this.rememberedWriterIndex = this.writerIndex;
    }

    /**
     * Resets the writer index to the previously marked index.
     */
    public void resetWriterIndex() {
        this.writerIndex = this.rememberedWriterIndex;
    }

    /**
     * Returns the number of writable bytes remaining in the buffer.
     * This is the number of bytes that can still be written without exceeding the buffer's capacity.
     *
     * @return the number of writable bytes.
     */
    public int writeableBytes() {
        return this.source.length - this.writerIndex;
    }

    /**
     * Checks if the buffer is writable (i.e., there is space to write).
     *
     * @return {@code true} if there is at least one writable byte, otherwise {@code false}.
     */
    public boolean isWriteable() {
        return isWriteable(0);
    }

    /**
     * Checks if the buffer has enough space to write the specified number of bytes.
     *
     * @param length the number of bytes to check for.
     * @return {@code true} if there is enough space to write the specified number of bytes, otherwise {@code false}.
     */
    public boolean isWriteable(int length) {
        return !isFixedSize() || this.writerIndex + length < this.size();
    }

    /**
     * Checks if the buffer is read-only.
     *
     * @return {@code true} if the buffer is read-only, {@code false} otherwise.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Checks if the buffer has a fixed size.
     *
     * @return {@code true} if the buffer has a fixed size, {@code false} otherwise.
     */
    public boolean isFixedSize() {
        return fixedSize;
    }

    /**
     * Returns the size of the buffer.
     *
     * @return the size of the buffer in bytes.
     */
    public int size() {
        return source.length;
    }

    /**
     * Returns the underlying byte array used by the buffer.
     *
     * @return the byte array representing the buffer's contents.
     */
    public byte[] getSource() {
        return source;
    }

}

