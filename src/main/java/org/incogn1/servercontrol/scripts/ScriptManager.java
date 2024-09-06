package org.incogn1.servercontrol.scripts;

import dev.dejvokep.boostedyaml.route.Route;
import org.incogn1.servercontrol.ServerControl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ScriptManager {

    /**
     * Runs a predefined script
     *
     * @param scriptPath the path to the script file, relative to the resources root for this plugin
     * @return a CompletableFuture that returns the exit code of the script as an integer
     *
     * @throws MissingScriptException when the script file could not be found
     */
    public CompletableFuture<Integer> runScript(Path scriptPath) throws MissingScriptException, IOException {

        Path fullPath = ServerControl.dataDirectory.resolve(scriptPath);
        Path parentDir = fullPath.getParent();
        String fileName = fullPath.getFileName().toString();

        // Guard - Script file must exist
        if (!Files.exists(fullPath)) {
            throw new MissingScriptException();
        }

        String execution = ServerControl.config.getString(Route.from("scripts", "execution")).replace("%scriptFile%", fileName);

        // Build and execute process
        Process process = Runtime.getRuntime().exec(execution, null, parentDir.toFile());

        // Redirect output if enabled in config
        boolean useOutputRedirect = ServerControl.config.getBoolean(Route.from("scripts", "use-output-redirect"));
        if (useOutputRedirect) {
            redirectOutput(process.getInputStream(), "SCRIPT-OUT");
            redirectOutput(process.getErrorStream(), "SCRIPT-ERR");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Redirects the output of a program to the velocity logger instance
     *
     * @param inputStream InputStream that should be redirected
     * @param prefix a descriptive prefix that is printed out in front of every line of redirected output
     */
    private void redirectOutput(InputStream inputStream, String prefix) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ServerControl.logger.info("[{}] {}", prefix, line);
                }
            } catch (IOException e) {
                ServerControl.logger.error("Error reading script output stream. {}", e.getMessage());
            }
        }).start();
    }
}
