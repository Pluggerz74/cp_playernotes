package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.model.AuditLogEntry;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class HistorySubCommand implements SubCommand {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public HistorySubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "history";
    }

    @Override
    public String permission() {
        return Permissions.HISTORY;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            messages.send(sender, "command.history-usage");
            return true;
        }

        OfflinePlayer target = CommandSupport.resolvePlayer(args[1]).orElse(null);
        if (target == null) {
            messages.send(sender, "command.player-not-found", Map.of("player", args[1]));
            return true;
        }

        String targetName = CommandSupport.displayName(target, args[1]);
        int limit = plugin.audit().maxHistoryResults();

        plugin.auditLogs().findByTarget(target.getUniqueId(), limit).whenComplete((entries, error) ->
                CommandSupport.runSync(plugin, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to load audit history for " + targetName, error);
                        messages.send(sender, "command.error");
                        return;
                    }

                    if (entries == null || entries.isEmpty()) {
                        messages.send(sender, "command.history-empty", Map.of("player", targetName));
                        return;
                    }

                    messages.send(sender, "command.history-header", Map.of(
                            "player", targetName,
                            "count", String.valueOf(entries.size())
                    ));

                    for (AuditLogEntry entry : entries) {
                        sender.sendMessage(messages.component("command.history-line", Map.of(
                                "id", String.valueOf(entry.getNoteId()),
                                "action", entry.getAction().name(),
                                "staff", entry.getStaffName(),
                                "date", DATE_TIME.format(entry.getCreatedAt())
                        )));
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
