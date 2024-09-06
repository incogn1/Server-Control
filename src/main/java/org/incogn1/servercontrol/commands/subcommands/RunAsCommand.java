package org.incogn1.servercontrol.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incogn1.servercontrol.commands.BaseCommand;
import org.incogn1.servercontrol.commands.SimpleCommandWithHelpMenuData;
import org.incogn1.servercontrol.commands.HelpMenuData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.incogn1.servercontrol.ServerControl.*;
import static org.incogn1.servercontrol.ServerControl.translationsManager;

/**
 * Command: run_as
 * <p>
 * Runs the command as another user.
 */
public class RunAsCommand implements SimpleCommandWithHelpMenuData {

    private final BaseCommand baseCommand;

    public RunAsCommand(BaseCommand baseCommand) {
        this.baseCommand = baseCommand;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Guard - Player argument must be given
        if (args.length <= 1) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage("commands.run_as.missing_player_arg")
            );
            return;
        }

        String playerName = args[1];
        Optional<Player> player = proxy.getPlayer(args[1]);

        // Guard - Player must exist
        if (player.isEmpty()) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "commands.run_as.player_not_found",
                    Map.of(
                        "player", playerName
                    )
                )
            );
            return;
        }

        // Change arguments to execute as player
        String[] proxyArgs = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new String[0];
        Invocation proxyInvocation = createProxyInvocation(player.get(), proxyArgs);

        source.sendMessage(
            translationsManager.translateAsMiniMessage(
                "commands.run_as.success",
                Map.of(
                    "command", "/sc " + String.join(" ", proxyArgs),
                    "player", playerName
                )
            )
        );

        // Run base command with proxy invocation
        baseCommand.execute(proxyInvocation);
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Need at least the 'run_as' permission
        if (!source.hasPermission("servercontrol.run_as")) {
            return false;
        }

        // Using subcommand
        if (args.length >= 3) {

            // Change arguments to suggest as base command
            String[] proxyArgs = Arrays.copyOfRange(args, 2, args.length);
            Invocation proxyInvocation = createProxyInvocation(source, proxyArgs);

            // Suggest as base command with proxy invocation
            return baseCommand.hasPermission(proxyInvocation);
        }

        // Not using subcommand yet
        return true;
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        String[] args = invocation.arguments();

        // List out all players
        if (args.length == 1) {
            List<String> playerNames = proxy.getAllPlayers().stream().map(Player::getUsername).toList();

            suggestions.addAll(playerNames);

            return suggestions;
        }

        // Selecting player
        if (args.length == 2) {
            String playerArg = args[1];
            List<String> playerNames = proxy.getAllPlayers().stream().map(Player::getUsername).toList();

            playerNames.forEach(playerName -> {
                if (playerName.toLowerCase().startsWith(playerArg.toLowerCase())) {
                    suggestions.add(playerName);
                }
            });

            return suggestions;
        }

        // Selecting subcommand
        if (args.length >= 3) {

            // Change arguments to suggest as base command
            String[] proxyArgs = Arrays.copyOfRange(args, 2, args.length);
            Invocation proxyInvocation = createProxyInvocation(null, proxyArgs);

            // Suggest as base command with proxy invocation
            return baseCommand.suggest(proxyInvocation);
        }

        // No suggestions
        return suggestions;
    }

    private Invocation createProxyInvocation(CommandSource source, String @NonNull [] args) {
        return new Invocation() {
            @Override
            public String alias() {
                return "";
            }

            @Override
            public CommandSource source() {
                return source;
            }

            @Override
            public String @NonNull [] arguments() {
                return args;
            }
        };
    }

    @Override
    public @NotNull HelpMenuData getHelpMenuData() {
        return new HelpMenuData(
            "Run As",
            "/sc run_as [player] [command]",
            new String[]{ "player", "command" },
            new String[]{ "[player]", "[command]" }
        );
    }
}
