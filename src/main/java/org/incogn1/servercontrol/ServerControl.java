package org.incogn1.servercontrol;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.incogn1.servercontrol.commands.BaseCommand;
import org.incogn1.servercontrol.resources.ResourceSync;
import org.incogn1.servercontrol.scripts.ScriptManager;
import org.incogn1.servercontrol.resources.translations.TranslationsManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.*;
import java.util.*;

@Plugin(
        id = "servercontrol",
        name = "ServerControl",
        version = "1.0-SNAPSHOT",
        authors = {"Incogn1"}
)
public class ServerControl {

    public static String PLUGIN_NAME = "ServerControl";
    public static String PLUGIN_VERSION = "1.0-SNAPSHOT";
    public static String[] PLUGIN_AUTHORS = {"Incogn1"};

    public static String CONFIG_FILE = "config.yml";
    public static String SCRIPTS_DIR = "scripts";

    public static Logger logger;
    public static ProxyServer proxy;
    public static YamlDocument config;
    public static Path dataDirectory;

    public static ScriptManager scriptManager;
    public static TranslationsManager translationsManager;
    public static ServerManager serverManager;

    @Inject
    public ServerControl(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        ServerControl.logger = logger;
        ServerControl.proxy = proxy;
        ServerControl.dataDirectory = dataDirectory;

        // Sync resources
        logger.debug("Syncing resources");
        try {
            ResourceSync.sync();
        } catch (Exception e) {
            logErrorMessage("Failed to sync resources! Shutting down plugin.", e);
            shutDown();
        }

        // Load config values
        logger.debug("Loading config");
        try {
            config = YamlDocument.create(
                    new File(dataDirectory.toFile(), CONFIG_FILE),
                    Objects.requireNonNull(getClass().getResourceAsStream("/" + CONFIG_FILE)),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder()
                            .setAutoUpdate(true)
                            .build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                            .build()
            );

            config.update();
            config.save();
        } catch (Exception e) {
            logErrorMessage("Failed to load plugin config! Shutting down plugin.", e);
            shutDown();
        }

        // Init ScriptManager
        logger.debug("Initializing ScriptManager");
        ServerControl.scriptManager = new ScriptManager();

        // Init TranslationsManager
        logger.debug("Initializing TranslationsManager");
        String locale = config.getString(Route.from("language"));
        try {
            ServerControl.translationsManager = new TranslationsManager(locale);
        } catch (FileNotFoundException e) {
            logger.error("Language file {}.json could not be found, please create the language file or change the language setting in the config! Shutting down plugin.", locale);
            shutDown();
        } catch (Exception e) {
            logErrorMessage("Failed to load translations! Shutting down plugin.", e);
            shutDown();
        }

        // Init ServerManager
        logger.debug("Initializing ServerManager");
        ServerControl.serverManager = new ServerManager();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        logger.debug("Registering commands...");

        // Register base command
        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("servercontrol")
                .aliases("sc")
                .plugin(this)
                .build();
        commandManager.register(commandMeta, new BaseCommand());

        logger.info("ServerControl plugin initialized!");
    }

    private void shutDown() {
        Optional<PluginContainer> container = proxy.getPluginManager().getPlugin("servercontrol");
        container.ifPresent(pluginContainer -> pluginContainer.getExecutorService().shutdown());
    }

    private void logErrorMessage(String msg, Exception e) {
        String exceptionMsg = e.getMessage();

        StringBuilder stackTraceStr = new StringBuilder();
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        for (int i = 0; i < stackTraceElements.length; i++) {
            stackTraceStr
                    .append("\n  - [")
                    .append(i + 1)
                    .append("] ")
                    .append(stackTraceElements[i]);
        }

        logger.error("{}\n\n Exception: \n  - {}\n\n StackTrace: {}\n", msg, exceptionMsg, stackTraceStr);
    }
}
