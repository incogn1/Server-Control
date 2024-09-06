package org.incogn1.servercontrol.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.incogn1.servercontrol.commands.SimpleCommandWithHelpMenuData;
import org.incogn1.servercontrol.commands.HelpMenuData;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.incogn1.servercontrol.ServerControl.*;

/**
 * Command: info
 * <p>
 * Returns some general info about the server.
 */
public class InfoCommand implements SimpleCommandWithHelpMenuData {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        String[] args = invocation.arguments();

        // Guard - Missing server argument
        if (args.length <= 1) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage("commands.info.missing_server_arg")
            );
            return;
        }

        String serverName = args[1];

        // Guard - Server must exist
        RegisteredServer server = serverManager.getServer(serverName);
        if (server == null) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "commands.info.server_not_found",
                    Map.of(
                        "server", serverName
                    )
                )
            );
            return;
        }

        // Startup script existence
        Path scriptPath = serverManager.getServerStartupScriptPath(serverName);
        boolean hasStartupScript = dataDirectory.resolve(scriptPath).toFile().exists();

        // Server online status
        boolean isOnline = serverManager.getServerOnlineState(server);

        // Clickable command to startup server in chat
        String runStartupLink = "<u><i><click:run_command:'/sc start " + serverName + "'>run script</click></i></u>";

        // Send information to command source
        source.sendMessage(
            MiniMessage.miniMessage().deserialize(
                "\n" +
                "<gray>---</gray> <b>Server info</b> <gray>---</gray>\n" +
                "\n" +
                "<gray>server: '<aqua>" + serverName + "</aqua>'</gray>\n" +
                "<gray>status: " + (isOnline ? "<green>online</green>" : "<red>offline</red>") + "</gray>\n" +
                "<gray>hasStartupScript: " + (hasStartupScript ? "<green>true</green> " + (isOnline ?  "" : runStartupLink) : "<red>false</red>") + "</gray>\n"
            )
        );
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("servercontrol.info");
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        String[] args = invocation.arguments();

        // Selecting server
        if (args.length == 2) {
            String serverArg = args[1];

            serverManager.getServers().forEach((name, server) -> {
                if (name.startsWith(serverArg)) {
                    suggestions.add(name);
                }
            });

            return suggestions;
        }

        // No suggestions
        return suggestions;
    }

    @Override
    public @NotNull HelpMenuData getHelpMenuData() {
        return new HelpMenuData(
            "Info",
            "/sc info [server]",
            new String[]{ "server" },
            new String[]{ "[server]" }
        );
    }
}
