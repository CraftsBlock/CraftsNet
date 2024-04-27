package de.craftsblock.craftsnet.api.http.body.bodies;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftscore.utils.Validator;
import de.craftsblock.craftsnet.api.http.Request;
import de.craftsblock.craftsnet.api.http.body.Body;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * The {@code JsonBody} class represents an HTTP request or response body containing JSON data.
 * It provides methods to parse and access JSON data.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.0
 * @see Body
 * @since CraftsNet-2.2.0
 */
public final class JsonBody extends Body {

    private final Json body;

    /**
     * Constructs a new {@code JsonBody} object by parsing a JSON-formatted string.
     *
     * @param request The representation of the http request.
     * @param body    The JSON-formatted string to parse.
     */
    public JsonBody(Request request, String body) {
        super(request);
        this.body = JsonParser.parse(body);
    }

    /**
     * Retrieves the JSON data contained in the body.
     *
     * @return The JSON data as a {@code Json} object.
     */
    public Json getBody() {
        return body;
    }

    /**
     * Parses an input stream as JSON and returns a {@code JsonBody} instance if the input is valid JSON.
     *
     * @param request The representation of the http request.
     * @param body    The input stream containing JSON data.
     * @return A {@code JsonBody} instance if the input is valid JSON, or {@code null} if it's not valid JSON.
     * @throws IOException If an error occurs while reading or parsing the input stream.
     */
    public static Body parseOrNull(Request request, InputStream body) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(body));
        StringBuilder lines = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            lines.append(line).append("\n");
        reader.close();
        if (Validator.isJsonValid(lines.toString())) return new JsonBody(request, lines.toString());
        return null;
    }

}
