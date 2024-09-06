package org.incogn1.servercontrol.resources.translations;

import com.google.gson.*;
import org.incogn1.servercontrol.ServerControl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TranslationsSync {

    // Relative to Data Directory
    private static final String BASE_DIR = "translations";

    // Relative to BASE_DIR
    private static final String MALFORMED_FILES_DIR = "malformed";

    // Relative to resources root
    private static final String DEFAULTS_RESOURCE_PATH = "translations/en.json";

    /**
     * Loops over all translation files to add missing entries and remove
     * entries that are not used. To determine if an entry is 'missing' or
     * 'not used' the translations/en.json file in this JAR's resources is
     * used as a reference.
     */
    public static void syncTranslations() throws IOException {
        File fullBaseDir = ServerControl.dataDirectory.resolve(BASE_DIR).toFile();

        File[] files = fullBaseDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (files == null) return;

        // Sync all files
        for (File file : files) {
            syncTranslationsFile(file);
        }
    }

    /**
     * Sync's a translation file with a matching reference
     * resource from this JAR (i.e. de.json would use de.json).
     * Missing entries  will be appended and entries that
     * do not exist in the reference will be removed.
     * <p>
     * If no matching reference resource could be found
     * the resource at DEFAULTS_RESOURCE_PATH is used
     * as a fallback.
     *
     * @param file the translations file to be synced
     */
    private static void syncTranslationsFile(File file) throws IOException {
        InputStream referenceInputStream = getReferenceInputStream(file);

        try (Reader referenceReader = new InputStreamReader(referenceInputStream)) {

            // Get reference translations JSON object
            JsonObject reference = JsonParser.parseReader(referenceReader).getAsJsonObject();

            // Get and sync translations JSON object from file contents
            JsonObject translations = loadAndSyncTranslationsJson(reference, file);

            // Set JSON Formatting rules
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

            // Save language file
            try (Writer writer = Files.newBufferedWriter(file.toPath())) {
                String jsonString = gson.toJson(translations);
                writer.write(jsonString);
            }
        }
    }

    /**
     * Loads and syncs the contents of the specified translations
     * file with the provided reference as "default values".
     *
     * @param reference the "default values" to be used when syncing
     * @param file the file to be loaded and synced
     * @return the synced file contents as a JsonObject
     */
    private static JsonObject loadAndSyncTranslationsJson(JsonObject reference, File file) throws IOException {
        JsonObject translations;

        // Try to sync translation entries. If translation file is malformed, back up file and use reference translations
        try (Reader translationsReader = Files.newBufferedReader(file.toPath())) {
            translations = JsonParser.parseReader(translationsReader).getAsJsonObject();

            // Add entries that exist in referenceTranslations but not in translations
            addMissingEntries(reference, translations);

            // Remove entries that exist in translations but not in referenceTranslations
            removeExtraEntries(reference, translations);
        } catch (IllegalStateException | JsonParseException e) {
            Path backupFilePath = backupMalformedTranslationsFile(file);

            ServerControl.logger.info("Translation file '{}' seems to be malformed, overwriting it with default values. The original malformed file has been backed up to '{}'", file.getPath(), backupFilePath);

            // Use default translations
            translations = reference;
        }

        return translations;
    }

    /**
     * Returns an InputStream to be used as "default" values for a
     * translations file. If a file with the same locale code as the
     * provided file parameter exists, that is used, else it will
     * default to the resource at DEFAULTS_RESOURCE_PATH.
     *
     * @param file the file for which a reference InputStream should
     *             be created.
     * @return the InputStream to be used as reference.
     */
    private static InputStream getReferenceInputStream(File file) throws IOException {
        ClassLoader classLoader = ServerControl.class.getClassLoader();

        InputStream matchingInputStream = classLoader.getResourceAsStream(file.getPath());

        if (matchingInputStream != null) return matchingInputStream;

        InputStream fallbackInputStream = classLoader.getResourceAsStream(DEFAULTS_RESOURCE_PATH);

        if (fallbackInputStream != null) return fallbackInputStream;

        throw new IOException("Could not initiate reference InputStream for file '" + file.getPath() + "'.");
    }

    /**
     * Consumes the entire reader and returns the contents as a string
     *
     * @param reader Reader instance to consume
     * @return the reader's contents as a String
     */
    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    /**
     * Backs up the provided file to the malformed translations directory
     *
     * @param file the file to be backed up
     * @return the relative path to the backed up file
     */
    private static Path backupMalformedTranslationsFile(File file) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String dateTimeString = LocalDateTime.now().format(formatter);

        Path filePath = file.toPath();

        Path relativeBackupFilePath = Paths.get(BASE_DIR, MALFORMED_FILES_DIR, dateTimeString + "-" + file.getName());
        Path backupFilePath = ServerControl.dataDirectory.resolve(relativeBackupFilePath);

        // Make sure parent directories exist
        Files.createDirectories(backupFilePath.getParent());

        // Use try-with-resources to ensure resources are closed
        try (BufferedReader reader = Files.newBufferedReader(filePath);
             BufferedWriter writer = Files.newBufferedWriter(backupFilePath)) {
            char[] buffer = new char[1024];
            int numRead;
            while ((numRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, numRead);
            }
        }

        return relativeBackupFilePath;
    }

    /**
     * Recursively walks through the target JsonObject
     * and copies any missing entries from the reference
     * JsonObject to the target.
     *
     * @param reference JsonObject with "default values"
     * @param target JsonObject with current values
     */
    public static void addMissingEntries(JsonObject reference, JsonObject target) {
        for (Map.Entry<String, JsonElement> entry : reference.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (!target.has(key)) {
                target.add(key, value);
            } else if (target.get(key).isJsonObject()) {
                // Recursively handle nested objects
                JsonObject referenceObject = value.getAsJsonObject();
                JsonObject targetObject = target.get(key).getAsJsonObject();
                addMissingEntries(referenceObject, targetObject);
            }
        }
    }

    /**
     * Recursively walks through the target JsonObject
     * and removes any entries that are not present in
     * the reference JsonObject.
     *
     * @param reference JsonObject with "default values"
     * @param target JsonObject with current values
     */
    public static void removeExtraEntries(JsonObject reference, JsonObject target) {
        boolean changes = true;
        while (changes) {
            changes = false;

            for (Map.Entry<String, JsonElement> entry : target.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                if (!reference.has(key)) {
                    target.remove(key);
                    changes = true;

                    // Target changed, restart the for loop
                    break;
                } else if (value.isJsonObject()) {
                    // Recursively handle nested objects
                    JsonObject referenceObject = reference.get(key).getAsJsonObject();
                    JsonObject targetObject = value.getAsJsonObject();
                    removeExtraEntries(referenceObject, targetObject);
                }
            }
        }
    }
}
