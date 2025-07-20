package de.craftsblock.craftsnet.api.http.body.bodies;

import de.craftsblock.craftsnet.api.http.Request;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code MultipartFormBody} class represents an HTTP request body that contains data in the
 * multipart/form-data format. This format is often used to upload files and form fields in HTTP
 * requests.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.1
 * @see FormBody
 * @since 2.2.0-SNAPSHOT
 */
public final class MultipartFormBody extends FormBody<MultipartFormBody.MultipartData> {

    private final String boundary;

    /**
     * Constructs a new {@code MultipartFormBody} by specifying the boundary and providing an input
     * stream containing the multipart/form-data.
     *
     * @param request  The representation of the http request.
     * @param boundary The boundary string that separates parts in the multipart request.
     * @param body     The input stream containing the multipart/form-data.
     * @throws IOException If an error occurs while reading or parsing the multipart data.
     */
    public MultipartFormBody(Request request, String boundary, InputStream body) throws IOException {
        super(request, body);
        this.boundary = "--" + boundary;
        deserialize();
    }

    /**
     * Deserializes the multipart form body, parsing it into individual parts and storing them in the data map.
     * This method reads the input stream and processes multipart form data, which consists of multiple parts
     * separated by a boundary. It extracts the name, content type, and data of each part and stores them in
     * the data map.
     *
     * @throws IOException If an I/O error occurs while reading the input stream.
     */
    @Override
    protected void deserialize() throws IOException {
        AtomicReference<String> name = new AtomicReference<>(); // AtomicReference to store the name of the current form field or file
        List<byte[]> values = new ArrayList<>(); // List to temporarily store the byte data of each part
        AtomicReference<String> contentType = new AtomicReference<>(); // AtomicReference to store the content type of the current part
        AtomicBoolean readHeader = new AtomicBoolean(true); // AtomicBoolean to track whether we are reading the header of a part
        List<byte[]> lines = splitBy(body.readAllBytes(), '\n'); // Read the entire body and split it into lines based on newline characters
        ConcurrentHashMap<String, ConcurrentLinkedQueue<MultipartItem>> storage = new ConcurrentHashMap<>(); // ConcurrentHashMap to store multipart items with the same name

        for (byte[] line : lines) {
            String stringified = new String(line, StandardCharsets.UTF_8);

            // Check if a new part starts with the boundary
            if (stringified.toLowerCase().startsWith(boundary.toLowerCase())) {
                if (!values.isEmpty()) {
                    // Create a new MultipartItem and add it to the storage
                    storage.computeIfAbsent(name.get(), s -> new ConcurrentLinkedQueue<>())
                            .add(new MultipartItem(List.copyOf(values), contentType.get()));
                }

                // Reset temporary variables
                name.set(null);
                values.clear();
                contentType.set(null);
                readHeader.set(true);
                continue;
            }

            // Typically a blank line symbolizes that the header is over.
            // So when we detect a blank line we can stop parsing the headers.
            if (stringified.isBlank() && readHeader.get()) {
                readHeader.set(false);
                continue;
            }

            if (readHeader.get()) {
                // Parse the header to extract the field or file name and content type
                if (stringified.startsWith("Content-Disposition:")) {
                    String[] disposition = stringified.split("; ");
                    if (disposition.length < 2) continue;
                    String inputName = disposition[1].split("=")[1];

                    Pattern pattern = Pattern.compile("[^a-zA-Z0-9-_:&;]");
                    Matcher matcher = pattern.matcher(inputName);

                    name.set(matcher.replaceAll(""));
                } else if (stringified.startsWith("Content-Type:")) contentType.set(stringified.substring(14));
            } else if (name.get() != null)
                // Add the line to the values list, which contains the data for the current part
                values.add(line);
        }

        // Populate the data map with the multipart items
        for (Map.Entry<String, ConcurrentLinkedQueue<MultipartItem>> item : storage.entrySet())
            data.computeIfAbsent(
                    item.getKey(),
                    s -> new MultipartData(item.getKey(), List.copyOf(item.getValue()))
            );
        storage.clear();
        body.close();
    }

    /**
     * Splits a byte array into multiple subarrays based on a separator byte.
     *
     * @param bytes     The byte array to split.
     * @param separator The byte used as the separator to split the array.
     * @return A list of byte arrays, each containing a segment of the original array separated by the given byte.
     */
    private List<byte[]> splitBy(byte[] bytes, char separator) {
        List<byte[]> byteList = new ArrayList<>(); // Create a list to store the resulting byte arrays
        int startIndex = 0; // Initialize the start index for subarray extraction

        // Loop through the input byte array
        for (int i = 0; i < bytes.length; i++)
            // Check if the current byte is the separator
            if (bytes[i] == separator) {
                int length = i - startIndex + 1; // Calculate the length of the subarray
                byte[] subArray = new byte[length]; // Create a new byte array to hold the subarray
                System.arraycopy(bytes, startIndex, subArray, 0, length); // Copy the segment between startIndex and i (inclusive) into the subarray
                byteList.add(subArray); // Add the subarray to the result list
                startIndex = i + 1; // Update the start index for the next subarray
            }

        // If there's any remaining data after the last separator, add it as the last subarray
        if (startIndex < bytes.length) {
            int length = bytes.length - startIndex; // Calculate the length of the remaining data
            byte[] subArray = new byte[length]; // Create a new byte array to hold the remaining data
            System.arraycopy(bytes, startIndex, subArray, 0, length); // Copy the remaining data into the subarray
            byteList.add(subArray); // Add the subarray to the result list
        }

        return byteList; // Return the list of split byte arrays
    }

    /**
     * Checks if a specific field exists in the multipart data.
     *
     * @param name The name of the field to check.
     * @return {@code true} if the field exists, otherwise {@code false}.
     */
    @Override
    public boolean hasField(String name) {
        return data.containsKey(name);
    }

    /**
     * Retrieves the multipart data for a specific field.
     *
     * @param name The name of the field to retrieve.
     * @return The multipart data for the field, or {@code null} if the field does not exist.
     */
    @Override
    public MultipartData getField(String name) {
        return data.getOrDefault(name, null);
    }

    /**
     * Gets the boundary string used to separate parts in the multipart request.
     *
     * @return The boundary string.
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * The {@code MultipartData} class represents the multipart data for a single field in the
     * multipart/form-data.
     *
     * @author CraftsBlock
     * @see MultipartFormBody
     * @see MultipartItem
     * @since 2.2.0
     */
    public record MultipartData(String name, List<MultipartItem> items) {

        /**
         * Gets the number of items in the multipart data for the field.
         *
         * @return The number of items.
         */
        public int size() {
            return items.size();
        }

        /**
         * Checks if the multipart data for the field is empty.
         *
         * @return {@code true} if it's empty, otherwise {@code false}.
         */
        public boolean isEmpty() {
            return items.isEmpty();
        }

        /**
         * Gets the first item in the multipart data for the field.
         *
         * @return The first item, or {@code null} if the data is empty.
         */
        public MultipartItem first() {
            if (!items.isEmpty()) return items.get(0);
            return null;
        }

    }

    /**
     * The {@code MultipartItem} class represents an individual part of the multipart data, which
     * can be a file or a form field.
     *
     * @author CraftsBlock
     * @see MultipartFormBody
     * @see MultipartData
     * @since 2.2.0
     */
    public record MultipartItem(List<byte[]> data, String contentType) {

        /**
         * Converts the multipart data to a string.
         *
         * @return The multipart data as a string.
         */
        public String getAsString() {
            StringBuilder builder = new StringBuilder();
            data.forEach(bytes -> builder.append(new String(bytes, StandardCharsets.UTF_8)).append("\n"));
            return builder.substring(0, builder.toString().length() - 1);
        }

        /**
         * Checks if the multipart item represents a file.
         *
         * @return {@code true} if it's a file, otherwise {@code false}.
         */
        public boolean isFile() {
            return contentType != null;
        }

        /**
         * Gets the file extension for the multipart item.
         *
         * @return The file extension.
         * @throws MimeTypeException If the content type is not recognized.
         */
        public String getFileExtension() throws Exception {
            MimeTypes types = MimeTypes.getDefaultMimeTypes();
            return types.forName(contentType).getExtension();
        }

        /**
         * Converts the multipart data to a file.
         *
         * @return The multipart data as a temporary file.
         * @throws MimeTypeException If the content type is not recognized.
         * @throws IOException       If an error occurs while creating the file or writing data to it.
         */
        public File getAsFile() throws Exception {
            return getAsFile(null);
        }

        /**
         * Converts the multipart data to a file with a specified extension.
         *
         * @param extension The file extension to use.
         * @return The multipart data as a temporary file.
         * @throws IOException       If an error occurs while creating the file or writing data to it.
         * @throws MimeTypeException If the content type is not recognized.
         */
        public File getAsFile(String extension) throws Exception {
            File file = File.createTempFile("craftsnet", extension == null ? getFileExtension() : (extension.startsWith(".") ? "" : ".") + extension);
            file.deleteOnExit();

            try (FileOutputStream stream = new FileOutputStream(file)) {
                for (byte[] line : data) stream.write(line);
                stream.flush();
            }

            return file;
        }

        /**
         * Validates the content type of the multipart item by comparing it to the actual data.
         *
         * @return {@code true} if the content type matches the data, otherwise {@code false}.
         * @throws MimeTypeException If the content type is not recognized.
         * @throws IOException       If an error occurs while reading the file.
         */
        public boolean validateContentType() throws Exception {
            try (FileInputStream stream = new FileInputStream(getAsFile())) {
                MimeTypes types = MimeTypes.getDefaultMimeTypes();
                return types.forName(contentType).matches(stream.readAllBytes());
            }
        }

    }

}
