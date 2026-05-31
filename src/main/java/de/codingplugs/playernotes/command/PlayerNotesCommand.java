package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class PlayerNotesCommand implements CommandExecutor, TabCompleter {

    private final MessageService messages;
    private final Map<String, SubCommand> subCommands;

    public PlayerNotesCommand(MessageService messages, List<SubCommand> commands) {
        this.messages = messages;
        this.subCommands = new LinkedHashMap<>();

        for (SubCommand subCommand : commands) {
            subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "command.usage");
            return true;
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        SubCommand subCommand = subCommands.get(input);
        if (subCommand == null) {
            messages.send(sender, "command.unknown");
            return true;
        }

        if (!sender.hasPermission(subCommand.permission())) {
            messages.send(sender, "command.no-permission");
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return subCommands.values().stream()
                    .filter(sub -> sub.name().startsWith(prefix))
                    .filter(sub -> sender.hasPermission(sub.permission()))
                    .map(SubCommand::name)
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subCommand != null && sender.hasPermission(subCommand.permission())) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return Collections.emptyList();
    }
}
