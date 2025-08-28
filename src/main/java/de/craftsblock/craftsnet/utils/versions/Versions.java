package de.craftsblock.craftsnet.utils.versions;

import de.craftsblock.craftscore.json.Json;
import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.logging.Logger;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling version comparison logic in the CNet system.
 * <p>
 * This class provides methods for determining whether a version string matches
 * a specified comparison operator and compares it to another version.
 * It leverages the {@link Comparison} enum to execute the comparisons.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.2
 * @see Comparison
 * @since 3.1.0-SNAPSHOT
 */
public class Versions {

    /**
     * Private constructor to prevent direct instantiation
     */
    private Versions() {
    }

    /**
     * Determines whether the given current version is suitable in comparison to the expected version.
     *
     * @param current  the current version as a string
     * @param expected the expected version as a string, which can optionally contain a comparison operator
     * @return {@code true} if the current version satisfies the comparison with the expected version, {@code false} otherwise
     */
    public static boolean suitable(String current, String expected) {
        Comparison comparison = extractOperator(expected);
        return comparison.suitable(cleanVersion(current), cleanVersion(expected));
    }

    /**
     * Cleans the version string by removing all non-numeric characters except for dots.
     *
     * @param version the version string to be cleaned
     * @return the cleaned version string containing only digits and dots
     */
    private static String cleanVersion(String version) {
        return version.replaceAll("[^\\d.]", "");
    }

    /**
     * Extracts the comparison operator from the beginning of the version string.
     *
     * @param version the version string, which may contain a comparison operator at the beginning
     * @return the {@link Comparison} enum representing the extracted comparison operator
     */
    private static Comparison extractOperator(String version) {
        Pattern pattern = Pattern.compile("^(<=|>=|<|>|=)");
        Matcher matcher = pattern.matcher(version.substring(0, Math.min(version.length(), 2)));

        String operator;
        if (matcher.find()) operator = matcher.group();
        else operator = "=";

        return Comparison.from(operator);
    }

    /**
     * Checks the version of CraftsNet using the repo server and displays the result in the console.
     *
     * @param craftsNet The current {@link CraftsNet} instance.
     */
    public static void verbalCheck(CraftsNet craftsNet) {
        Logger logger = craftsNet.getLogger();
        logger.info("Checking for new version...");
        try {
            URL url = new URL("https://repo.craftsblock.de/api/maven/latest/version/releases/de/craftsblock/craftsnet?type=json");
            try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8)) {
                String next = scanner.next();
                Json json = JsonParser.parse(next);
                if (json.contains("status") && json.getInt("status") != 200) {
                    logger.error("Failed to fetch the latest version of CraftsNet!");
                    return;
                }

                if (Versions.suitable(CraftsNet.version, ">=" + json.getString("version"))) {
                    logger.info("You are using the newest version");
                    return;
                }

                logger.warning("There is a newer version (%s) of CraftsNet available", json.getString("version"));
            }
        } catch (Exception e) {
            logger.error("Failed to fetch the latest version of CraftsNet!", e);
        }
    }

}
