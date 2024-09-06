package org.incogn1.servercontrol.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.dejvokep.boostedyaml.route.Route;
import org.incogn1.servercontrol.commands.subcommands.HelpCommand;
import org.incogn1.servercontrol.commands.subcommands.*;

import java.util.*;

import static org.incogn1.servercontrol.ServerControl.*;

public class BaseCommand implements SimpleCommand {

    private final Map<String, SimpleCommandWithHelpMenuData> commands;

    public BaseCommand() {
        this.commands = new LinkedHashMap<>();

        // Add all commands to map
        commands.put("help", null); // Just here for ordering purposes, replaced later
        commands.put("list", new ListCommand());
        commands.put("info", new InfoCommand());
        commands.put("start", new StartCommand());
        commands.put("join", new JoinCommand());
        commands.put("cancel_join", new CancelJoinCommand());
        commands.put("run_as", new RunAsCommand(this));

        // Help command
        commands.replace("help", new HelpCommand(commands));
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Base command
        if (args.length == 0) {
            String authors = String.join(", ", PLUGIN_AUTHORS);

            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "commands.base.no_sub_command",
                    Map.of(
                        "plugin", PLUGIN_NAME,
                        "version", PLUGIN_VERSION,
                        "authors", authors
                    )
                )
            );

            return;
        }

        String command = args[0];

        // Subcommand
        if (commands.containsKey(command)) {
            commands.get(command).execute(invocation);
        } else {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "commands.base.unknown_sub_command",
                    Map.of(
                        "command", command
                    )
                )
            );
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        if (!config.getBoolean(Route.from("use-permissions"))) {
            return true; // Permissions disabled in config
        }

        String[] args = invocation.arguments();

        // Need at least base permission
        if (!invocation.source().hasPermission("servercontrol.base")) {
            return false;
        }

        // Base command
        if (args.length == 0) {
            return true;
        }

        String command = args[0];

        // Existing subcommand
        if (commands.containsKey(command)) {
            return commands.get(command).hasPermission(invocation);
        }

        // Incorrect subcommand
        return true;
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        String[] args = invocation.arguments();

        // List out all subcommands
        if (args.length == 0) {
            suggestions.addAll(commands.keySet());

            return suggestions;
        }

        String commandArg = args[0];

        // Selecting specific subcommand
        if (args.length == 1) {
            commands.keySet().forEach(command -> {
                if (command.startsWith(commandArg)) {
                    suggestions.add(command);
                }
            });

            return suggestions;
        }

        // Subcommand suggestions
        if (commands.containsKey(commandArg)) {
            return commands.get(commandArg).suggest(invocation);
        }

        // No suggestions
        return suggestions;
    }
}
