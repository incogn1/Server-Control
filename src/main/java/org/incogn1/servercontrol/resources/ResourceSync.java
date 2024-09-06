package org.incogn1.servercontrol.resources;

import org.incogn1.servercontrol.ServerControl;
import org.incogn1.servercontrol.resources.translations.TranslationsSync;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ResourceSync {

    /**
     * Returns the predefined list of resources that should
     * be created when using the sync() method.
     *
     * @return a map of all resources in the form: Map(String path, Boolean isDirectory)
     */
    private static Map<String, Boolean> getResourcePaths() {

        // Map with all resources that should be created by default
        // Content: <path, isDirectory>
        Map<String, Boolean> paths = new HashMap<>();

        // List of all resources to add
        paths.put("scripts", true);
        paths.put("translations/en.json", false);
        paths.put("config.yml", false);

        return paths;
    }

    /**
     * Synchronizes the resources in the plugin's data directory by
     * copying files from the JAR's resources into the data directory.
     * <p>
     * Files or folders that already exist are skipped.
     */
    public static void sync() throws IOException {
        Map<String, Boolean> resourcePaths = getResourcePaths();

        for (Map.Entry<String, Boolean> entry : resourcePaths.entrySet()) {
            String path = entry.getKey();
            Boolean isDirectory = entry.getValue();

            Path filePath = ServerControl.dataDirectory.resolve(path);

            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                if (isDirectory) {
                    Files.createDirectory(filePath);
                } else {
                    // Copy default values from resource into file
                    try (InputStream resourceStream = ServerControl.class.getClassLoader().getResourceAsStream(path)) {
                        if (resourceStream == null) {
                            throw new IOException("Resource not found: " + path);
                        }
                        Files.copy(resourceStream, filePath);
                    }
                }
            }
        }

        // Sync translation entries
        TranslationsSync.syncTranslations();
    }
}
