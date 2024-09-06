package org.incogn1.servercontrol.resources.translations;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.incogn1.servercontrol.ServerControl;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class TranslationsManager {

    private final JsonObject translations;

    public TranslationsManager(String locale) throws IOException {
        Gson gson = new Gson();
        Path filePath = ServerControl.dataDirectory.resolve("translations/" + locale + ".json");

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException(filePath.toString());
        }

        FileReader reader = new FileReader(filePath.toFile());
        this.translations = gson.fromJson(reader, JsonObject.class);
        reader.close();
    }

    /**
     * Gets the translation from the correct file (as specified in
     * the config) and finally returns the localized string.
     *
     * @param path dot-separated path to entry (e.g. "entry", "parent.entry", "parent.parent.entry", etc.)
     * @return the translation to be used, as a string
     */
    public String translate(String path) {
        String translation = getTranslation(path);

        return Objects.requireNonNullElseGet(translation, () -> "Missing translation @ " + path);
    }

    /**
     * Gets the translation from the correct file (as specified in
     * the config), replaces placeholders with given values and
     * finally returns the localized string.
     *
     * @param path dot-separated path to entry (e.g. "entry", "parent.entry", "parent.parent.entry", etc.)
     * @param variables string values to replace in translations entry
     * @return the translation to be used, as a string
     */
    public String translate(String path, Map<String, String> variables) {
        String translation = getTranslation(path);
        if (translation == null) {
            return "Missing translation @ " + path;
        }

        // Replace variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            translation = translation.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        return translation;
    }

    /**
     * Gets the translation from the correct file (as specified in
     * the config) and finally returns the localized text component.
     *
     * @param path dot-separated path to entry (e.g. "entry", "parent.entry", "parent.parent.entry", etc.)
     * @return the translation to be used, as a deserialized MiniMessage text component
     */
    public Component translateAsMiniMessage(String path) {
        String translation = getTranslation(path);
        return MiniMessage.miniMessage().deserialize(
                Objects.requireNonNullElseGet(translation, () -> "Missing translation @ " + path)
        );
    }

    /**
     * Gets the translation from the correct file (as specified in
     * the config), replaces placeholders with given values and
     * finally returns the localized string.
     *
     * @param path dot-separated path to entry (e.g. "entry", "parent.entry", "parent.parent.entry", etc.)
     * @param variables string values to replace in translations entry
     * @return the translation to be used, as a deserialized MiniMessage text component
     */
    public Component translateAsMiniMessage(String path, Map<String, String> variables) {
        String translation = getTranslation(path);
        if (translation == null) {
            return MiniMessage.miniMessage().deserialize("Missing translation @ " + path);
        }

        // Replace variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            translation = translation.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        return MiniMessage.miniMessage().deserialize(translation);
    }

    /**
     * Checks if a translation exists
     *
     * @param path dot-separated path to entry (e.g. "entry", "parent.entry", "parent.parent.entry", etc.)
     * @return true if the translation at the given path exsits, else false
     */
    public boolean translationExists(String path) {
        return getTranslation(path) != null;
    }

    /**
     * Walks the translations JsonObject to get the specified
     * translation using a string as the path. Nested paths
     * can be specified by separating with a dot.
     * <p>
     * Example: getTranslation("parent.child");
     *
     * @param path dot-separated path to entry (e.g. "entry", "parent.entry", "parent.parent.entry", etc.)
     * @return entry as a string
     */
    private String getTranslation(String path) {
        String[] keys = path.split("\\.");
        JsonObject currentObject = translations;
        JsonElement value = null;

        for (String key : keys) {
            value = currentObject.get(key);
            if (value != null && value.isJsonObject()) {
                currentObject = value.getAsJsonObject();
            } else {
                break;
            }
        }

        return value != null ? value.getAsString() : null;
    }
}
