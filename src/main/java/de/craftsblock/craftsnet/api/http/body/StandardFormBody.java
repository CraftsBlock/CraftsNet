package de.craftsblock.craftsnet.api.http.body;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * The {@code StandardFormBody} class represents an HTTP request body that contains form data
 * encoded in the standard application/x-www-form-urlencoded format.
 * <p>
 * This class extends the {@link FormBody} class and provides methods to deserialize the form data
 * from an input stream and access individual form fields.
 *
 * @author CraftsBlock
 * @version 1.0
 * @see FormBody
 * @since 2.2.0
 */
public class StandardFormBody extends FormBody<String> {

    /**
     * Constructs a new {@code StandardFormBody} by reading and parsing form data from an input stream.
     *
     * @param body The input stream containing the form data.
     * @throws IOException If an error occurs while reading or parsing the form data.
     */
    public StandardFormBody(InputStream body) throws IOException {
        super(body);
    }

    /**
     * Deserializes the input stream, parsing it into individual form fields and values.
     * This method reads the input streamline by line, splitting it into form fields and their values.
     * It then decodes and stores the field names and values in the data map.
     *
     * @throws IOException If an I/O error occurs while reading the input stream.
     */
    @Override
    protected void deserialize() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(body)); // Create a buffered reader to read the input streamline by line
        StringBuilder lines = new StringBuilder(); // Create a StringBuilder to accumulate lines from the input stream
        String line;

        // Read the input streamline by line and append it to the StringBuilder
        while ((line = reader.readLine()) != null)
            lines.append(line).append("\n");

        String[] fields = lines.toString().split("&"); // Split the accumulated lines into individual form fields

        // Iterate through each form field and parse its name and value
        for (String field : fields) {
            String[] fieldData = field.split("="); // Split the field into its name and value based on the "=" character
            if (fieldData.length != 2) continue; // Check if the field has both a name and a value
            // Decode the URL-encoded value using UTF-8 encoding and remove the last 2 characters (typically newline characters)
            String decodedValue = URLDecoder.decode(fieldData[1], StandardCharsets.UTF_8).substring(0, fieldData[1].length() - 2);
            data.put(fieldData[0], decodedValue); // Store the field name and its decoded value in the data map
        }
        reader.close();
    }

    /**
     * Checks if a specific field exists in the form data.
     *
     * @param name The name of the field to check.
     * @return {@code true} if the field exists, otherwise {@code false}.
     */
    @Override
    public boolean hasField(String name) {
        return data.containsKey(name);
    }

    /**
     * Retrieves the value of a specific field from the form data.
     *
     * @param name The name of the field to retrieve.
     * @return The value of the field, or {@code null} if the field does not exist.
     */
    @Override
    public String getField(String name) {
        return data.getOrDefault(name, null);
    }

}