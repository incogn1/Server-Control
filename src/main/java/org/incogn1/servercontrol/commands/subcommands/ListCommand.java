package org.incogn1.servercontrol.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import org.incogn1.servercontrol.commands.SimpleCommandWithHelpMenuData;
import org.incogn1.servercontrol.commands.HelpMenuData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.incogn1.servercontrol.ServerControl.serverManager;
import static org.incogn1.servercontrol.ServerControl.translationsManager;

/**
 * Command: list
 * <p>
 * Returns a list of all servers on the network.
 */
public class ListCommand implements SimpleCommandWithHelpMenuData {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // Generate list of servers with /sc join [server] as click command
        StringBuilder serverList = new StringBuilder();
        for (String serverName : serverManager.getServers().keySet()) {
            serverList
                .append("<click:run_command:'/sc info ")
                .append(serverName)
                .append("'><aqua>")
                .append(serverName)
                .append("</aqua></click>")
                .append(", ");
        }
        serverList.delete(serverList.length() - 2, serverList.length());

        // Send server list
        source.sendMessage(
            translationsManager.translateAsMiniMessage(
                "commands.list.server_list",
                Map.of(
                    "servers", serverList.toString()
                )
            )
        );
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("servercontrol.list");
    }

    @Override
    public @NotNull HelpMenuData getHelpMenuData() {
        return new HelpMenuData(
            "List",
            "/sc list",
            new String[0],
            new String[0]
        );
    }
}
