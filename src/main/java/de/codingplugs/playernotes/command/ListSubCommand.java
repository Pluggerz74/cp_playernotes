package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import de.codingplugs.playernotes.util.NoteFormatter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class ListSubCommand implements SubCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public ListSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String permission() {
        return Permissions.VIEW;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            messages.send(sender, "command.list-usage");
            return true;
        }

        OfflinePlayer target = CommandSupport.resolvePlayer(args[1]).orElse(null);
        if (target == null) {
            messages.send(sender, "command.player-not-found", Map.of("player", args[1]));
            return true;
        }

        String targetName = CommandSupport.displayName(target, args[1]);

        plugin.notes().findByTarget(target.getUniqueId(), false).whenComplete((notes, error) ->
                CommandSupport.runSync(plugin, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to list notes for " + targetName, error);
                        messages.send(sender, "command.error");
                        return;
                    }

                    messages.send(sender, "command.list-header", Map.of(
                            "player", targetName,
                            "count", String.valueOf(notes.size())
                    ));

                    if (notes.isEmpty()) {
                        messages.send(sender, "command.list-empty", Map.of("player", targetName));
                        return;
                    }

                    for (var note : notes) {
                        sender.sendMessage(MINI_MESSAGE.deserialize(NoteFormatter.formatListLine(note)));
                    }
                }));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandSupport.tabCompleteOnlinePlayers(args[1]);
        }
        return List.of();
    }
}
