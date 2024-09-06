package org.incogn1.servercontrol.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.incogn1.servercontrol.commands.SimpleCommandWithHelpMenuData;
import org.incogn1.servercontrol.commands.HelpMenuData;
import org.jetbrains.annotations.NotNull;

import static org.incogn1.servercontrol.ServerControl.serverManager;
import static org.incogn1.servercontrol.ServerControl.translationsManager;

/**
 * Command: cancel_join
 * <p>
 * Cancels any delayed join that the player might have active.
 */
public class CancelJoinCommand implements SimpleCommandWithHelpMenuData {

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

        // Cancel auto join event
        serverManager.cancelDelayedPlayerJoin(player);

        // Cancel notifications
        serverManager.cancelSourceNotifications(source);

        source.sendMessage(
            translationsManager.translateAsMiniMessage("commands.cancel_join.cancelled_delayed_join")
        );
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("servercontrol.join");
    }

    @Override
    public @NotNull HelpMenuData getHelpMenuData() {
        return new HelpMenuData(
            "Cancel Join",
            "/sc cancel_join",
            new String[0],
            new String[0]
        );
    }
}
