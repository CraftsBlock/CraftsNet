package de.craftsblock.craftsnet.api.http.encoding;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A parser for the Accept-Encoding http header. This parser extracts encoding types and their
 * associated quality values (q-values), sorts them by priority, and returns a structured representation.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.3-SNAPSHOT
 */
public final class AcceptEncodingHelper {

    /**
     * Private constructor to prevent external instantiation.
     */
    private AcceptEncodingHelper() {
    }

    /**
     * Parses an Accept-Encoding header and extracts encoding types with their quality values.
     *
     * @param header The Accept-Encoding header string.
     * @return A list of encoding types and their q-values, sorted by priority (highest q-value first).
     */
    public static List<String> parseHeader(String header) {
        List<Map.Entry<String, Double>> encodings = new ArrayList<>();
        String[] parts = header.split(",");

        for (String part : parts) {
            String[] tokens = part.trim().split(";");
            String encoding = tokens[0].trim();
            double prio = 1.0;

            if (tokens.length > 1)
                for (String token : tokens) {
                    token = token.trim();
                    if (!token.startsWith("q=")) continue;

                    try {
                        prio = Double.parseDouble(token.substring(2));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                }

            encodings.add(new AbstractMap.SimpleEntry<>(encoding, prio));
        }

        encodings.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return encodings.stream().map(Map.Entry::getKey).toList();
    }

}
