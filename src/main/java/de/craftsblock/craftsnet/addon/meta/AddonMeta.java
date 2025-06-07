package de.craftsblock.craftsnet.addon.meta;

import com.google.gson.JsonElement;
import de.craftsblock.craftscore.json.Json;
import org.jetbrains.annotations.NotNull;

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
 * @version 1.0.2
 * @since 3.1.0-SNAPSHOT
 */
public record AddonMeta(String name, String mainClass, String description, List<String> authors, String website, String version, String[] depends) {

    /**
     * Creates an {@link AddonMeta} instance from the configuration provided by the {@link AddonConfiguration}.
     *
     * @param configuration The configuration object containing the metadata in json format.
     * @return A new instance of {@link AddonMeta} populated with the values from the configuration.
     */
    public static AddonMeta of(@NotNull AddonConfiguration configuration) {
        if (configuration.json() == null)
            throw new IllegalStateException("The addon json config is null!");

        Json json = configuration.json();

        List<String> authors = new ArrayList<>();
        addMultiple(authors, json, "author");
        addMultiple(authors, json, "authors");

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
     * Adds strings to the provided list based on a path in the json object. The path may represent
     * a single representation or an array of strings.
     *
     * @param destination The list to which the strings should be added.
     * @param json        The json object containing the author information.
     * @param path        The json path that represents the author or strings.
     */
    private static void addMultiple(List<String> destination, Json json, String path) {
        if (!json.contains(path)) return;
        JsonElement element = json.get(path);

        if (element.isJsonPrimitive()) {
            String value = json.getString(path);
            if (destination.contains(value)) return;
            destination.add(value);
            return;
        }

        if (!element.isJsonArray()) return;

        for (JsonElement raw : element.getAsJsonArray()) {
            if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString()) continue;
            String value = raw.getAsJsonPrimitive().getAsString();
            if (destination.contains(value)) continue;
            destination.add(value);
        }
    }

}
