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

public final class ViewPlayerCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public ViewPlayerCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!CommandSupport.hasPermission(sender, Permissions.VIEW)) {
            messages.send(sender, "command.no-permission");
            return true;
        }

        if (args.length < 1 || args[0].isBlank()) {
            messages.send(sender, "command.view-usage");
            return true;
        }

        OfflinePlayer target = CommandSupport.resolvePlayer(args[0]).orElse(null);
        if (target == null) {
            messages.send(sender, "command.player-not-found", Map.of("player", args[0]));
            return true;
        }

        String targetName = CommandSupport.displayName(target, args[0]);

        plugin.notes().findByTarget(target.getUniqueId(), false).whenComplete((notes, error) ->
                CommandSupport.runSync(plugin, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to load notes for " + targetName, error);
                        messages.send(sender, "command.error");
                        return;
                    }

                    int criticalCount = (int) notes.stream().filter(note -> note.isCritical()).count();

                    messages.send(sender, "command.view-header", Map.of(
                            "player", targetName,
                            "count", String.valueOf(notes.size()),
                            "critical", String.valueOf(criticalCount)
                    ));

                    if (notes.isEmpty()) {
                        messages.send(sender, "command.view-empty", Map.of("player", targetName));
                        return;
                    }

                    for (var note : notes) {
                        sender.sendMessage(MINI_MESSAGE.deserialize(NoteFormatter.formatSummaryLine(note)));
                    }
                }));

        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!CommandSupport.hasPermission(sender, Permissions.VIEW)) {
            return List.of();
        }

        if (args.length == 1) {
            return CommandSupport.tabCompleteOnlinePlayers(args[0]);
        }

        return List.of();
    }
}
