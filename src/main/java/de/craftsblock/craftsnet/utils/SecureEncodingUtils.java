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
 * @version 1.0.0
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

}
