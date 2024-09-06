package org.incogn1.servercontrol.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.incogn1.servercontrol.commands.SimpleCommandWithHelpMenuData;
import org.incogn1.servercontrol.commands.HelpMenuData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.incogn1.servercontrol.ServerControl.*;

/**
 * Command: start
 * <p>
 * When called, pings the server to see if it is online.
 * <p>
 * If the server is already online, nothing happens.
 * <p>
 * If the server is offline, the server will be started.
 */
public class StartCommand implements SimpleCommandWithHelpMenuData {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        String[] args = invocation.arguments();

        // Guard - Missing server argument
        if (args.length <= 1) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage("commands.start.missing_server_arg")
            );
            return;
        }

        String serverName = args[1];

        // Guard - Server must exist
        RegisteredServer server = serverManager.getServer(serverName);
        if (server == null) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "commands.start.server_not_found",
                    Map.of(
                        "server", serverName
                    )
                )
            );
            return;
        }

        boolean isOnline = serverManager.getServerOnlineState(server);

        // Guard - Server cannot already be online
        if (isOnline) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "commands.start.server_already_online",
                    Map.of(
                        "server", serverName
                    )
                )
            );

            return;
        }

        // Run startup script
        serverManager.startServerWithNotify(serverName, source);
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        String[] args = invocation.arguments();

        // Need at least the 'start' permission
        if (!invocation.source().hasPermission("servercontrol.start")) {
            return false;
        }

        // Also need permission for specific server (if the given server exists)
        if (args.length >= 2) {
            String serverArg = args[1];

            if (serverManager.getServer(serverArg) != null) {
                return invocation.source().hasPermission("servercontrol.start." + serverArg);
            } else {
                return true;
            }
        }

        return true;
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
            "Start",
            "/sc start [server]",
            new String[]{ "server" },
            new String[]{ "[server]" }
        );
    }
}
