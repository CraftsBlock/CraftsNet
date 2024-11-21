package de.craftsblock.craftsnet.addon;

import com.google.gson.JsonElement;
import de.craftsblock.craftscore.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents metadata information for an addon, such as its name, main class, authors, website, version, and dependencies.
 * This metadata is typically loaded from a configuration file in JSON format.
 *
 * @param name        The name of the addon.
 * @param mainClass   The main class responsible for initializing the addon.
 * @param description The description of the addon.
 * @param authors     A list of authors of the addon.
 * @param website     The website associated with the addon.
 * @param version     The version of the addon.
 * @param depends     The dependencies required by the addon.
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.1
 * @since 3.0.7-SNAPSHOT
 */
public record AddonMeta(String name, String mainClass, String description, List<String> authors, String website, String version, String[] depends) {

    /**
     * Creates an {@link AddonMeta} instance from the configuration provided by the {@link AddonLoader.Configuration}.
     *
     * @param configuration The configuration object containing the metadata in json format.
     * @return A new instance of {@link AddonMeta} populated with the values from the configuration.
     */
    static AddonMeta of(AddonLoader.Configuration configuration) {
        Json json = configuration.json();

        List<String> authors = new ArrayList<>();
        addAuthors(authors, json, "author");
        addAuthors(authors, json, "authors");

        return new AddonMeta(
                json.getString("name"),
                json.getString("main"),
                Optional.ofNullable(json.getString("description")).orElse(""),
                authors,
                Optional.ofNullable(json.getString("website")).orElse(""),
                Optional.ofNullable(json.getString("version")).orElse(""),
                Optional.ofNullable(json.getStringList("depends").toArray(String[]::new)).orElse(new String[0])
        );
    }

    /**
     * Adds authors to the provided list based on a path in the json object. The path may represent
     * a single author or an array of authors.
     *
     * @param authors The list to which the authors should be added.
     * @param json    The json object containing the author information.
     * @param path    The json path that represents the author or authors.
     */
    private static void addAuthors(List<String> authors, Json json, String path) {
        if (!json.contains(path)) return;
        JsonElement element = json.get(path);

        if (element.isJsonPrimitive()) {
            String author = json.getString(path);
            if (authors.contains(author)) return;
            authors.add(author);
            return;
        }

        if (!element.isJsonArray()) return;

        for (JsonElement rawAuthor : element.getAsJsonArray()) {
            if (!rawAuthor.isJsonPrimitive() || !rawAuthor.getAsJsonPrimitive().isString()) continue;
            String author = rawAuthor.getAsJsonPrimitive().getAsString();
            if (authors.contains(author)) continue;
            authors.add(author);
        }
    }

}
