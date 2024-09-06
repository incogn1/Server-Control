package org.incogn1.servercontrol.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.incogn1.servercontrol.commands.SimpleCommandWithHelpMenuData;
import org.incogn1.servercontrol.commands.HelpMenuData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.incogn1.servercontrol.ServerControl.*;

/**
 * Command: join
 * <p>
 * When called, pings the server to see if it is online.
 * <p>
 * If the server is already online, the player that executed
 * the command will be sent to the server.
 * <p>
 * If the server is offline, the server will first be started
 * using the start.bat specified in the plugin's config. A
 * separate thread will be started that watches for the server
 * to start. After the server has successfully started, the
 * player will be sent to it (unless they have used this command
 * to join another server in the meantime).
 */
public class JoinCommand implements SimpleCommandWithHelpMenuData {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // Guard - Only players can use command
        if (!(source instanceof Player player)) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage("commands.requires_player_source")
            );
            return;
        }

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

        boolean isOnline = serverManager.getServerOnlineState(serverName);

        // ---
        // Case A - Server is online -> Immediate join
        // ---
        if (isOnline) {
            player.createConnectionRequest(server).connectWithIndication();
            return;
        }

        // ---
        // Case B - Server is offline -> Startup request + delayed join
        // ---

        // Set delayed player join
        serverManager.setDelayedPlayerJoin(player, serverName);

        // Run startup script
        serverManager.startServerWithNotify(serverName, source);
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        String[] args = invocation.arguments();

        // Need at least the 'start' permission
        if (!invocation.source().hasPermission("servercontrol.join")) {
            return false;
        }

        // Also need permission for specific server (if the given server exists)
        if (args.length >= 2) {
            String serverArg = args[1];

            if (serverManager.getServer(serverArg) != null) {
                return invocation.source().hasPermission("servercontrol.join." + serverArg);
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
            "Join",
            "/sc join [server]",
            new String[]{ "server" },
            new String[]{ "[server]" }
        );
    }
}
