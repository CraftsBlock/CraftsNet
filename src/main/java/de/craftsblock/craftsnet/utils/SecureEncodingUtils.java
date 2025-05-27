package de.craftsblock.craftsnet.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * Utility class for secure encoding and decoding of character arrays to byte arrays and vice versa.
 * <p>
 * This class provides methods to convert between char[] and byte[] while minimizing sensitive data
 * retention in memory by clearing intermediate buffers after use.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.1
 * @since 3.4.1-SNAPSHOT
 */
public class SecureEncodingUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private SecureEncodingUtils() {
    }

    /**
     * Encodes a given array of chars into a byte array.
     *
     * @param chars the input character array to encode
     * @return A byte array containing the bytes of the input characters
     * @throws RuntimeException If the encoding fails
     */
    public static byte[] encode(char[] chars, Charset charset) {
        CharsetEncoder encoder = charset.newEncoder();
        CharBuffer charBuffer = CharBuffer.wrap(chars);

        try {
            int maxBytes = (int) (encoder.maxBytesPerChar() * chars.length);
            java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(maxBytes);

            CoderResult encoded = encoder.encode(charBuffer, byteBuffer, true);
            if (!encoded.isUnderflow()) encoded.throwException();

            CoderResult result = encoder.flush(byteBuffer);
            if (!result.isUnderflow()) result.throwException();

            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.limit()];
            byteBuffer.get(bytes);

            // Clear buffer to minimize sensitive data retention
            byteBuffer.clear();
            for (int i = 0; i < maxBytes; i++)
                byteBuffer.put((byte) 0);

            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("UTF-8 encoding failed", e);
        }
    }

    /**
     * Decodes the given byte array into a char array using the specified charset.
     *
     * @param bytes   The byte array containing the encoded characters; must not be null
     * @param charset The charset to use for decoding; must not be null
     * @return A char array containing the decoded characters
     * @throws RuntimeException     If decoding fails for any reason
     * @throws NullPointerException If bytes or charset is null
     */
    public static char[] decode(byte[] bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        try {
            int maxChars = (int) (decoder.maxCharsPerByte() * bytes.length);
            CharBuffer charBuffer = CharBuffer.allocate(maxChars);

            CoderResult decoded = decoder.decode(byteBuffer, charBuffer, true);
            if (!decoded.isUnderflow()) decoded.throwException();

            CoderResult result = decoder.flush(charBuffer);
            if (!result.isUnderflow()) result.throwException();

            charBuffer.flip();
            char[] chars = new char[charBuffer.limit()];
            charBuffer.get(chars);

            // Clear buffer to minimize sensitive data retention
            charBuffer.clear();
            for (int i = 0; i < maxChars; i++)
                charBuffer.put('\0');

            return chars;
        } catch (Exception e) {
            throw new RuntimeException("Decoding failed", e);
        }
    }

}
