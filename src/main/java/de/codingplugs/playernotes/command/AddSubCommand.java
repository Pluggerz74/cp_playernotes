package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.model.NoteType;
import de.codingplugs.playernotes.model.PlayerNote;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AddSubCommand implements SubCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public AddSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    public String permission() {
        return Permissions.ADD;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "command.add-usage");
            return true;
        }

        OfflinePlayer target = CommandSupport.resolvePlayer(args[1]).orElse(null);
        if (target == null) {
            messages.send(sender, "command.player-not-found", Map.of("player", args[1]));
            return true;
        }

        String content = CommandSupport.joinArgs(args, 2);
        if (content.isBlank()) {
            messages.send(sender, "command.add-usage");
            return true;
        }

        CommandSupport.StaffIdentity staff = CommandSupport.staffIdentity(sender);
        String targetName = CommandSupport.displayName(target, args[1]);
        Instant now = Instant.now();

        PlayerNote note = new PlayerNote(
                0L,
                target.getUniqueId(),
                targetName,
                staff.uuid(),
                staff.name(),
                NoteType.INFO,
                NotePriority.NORMAL,
                content,
                now,
                now,
                false
        );

        plugin.notes().createNote(note).whenComplete((created, error) ->
                CommandSupport.deliverFeedback(plugin, sender, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to create note for " + targetName, error);
                        messages.send(sender, "command.database-error");
                        return;
                    }

                    if (created == null) {
                        plugin.logSevere("Failed to create note for " + targetName, new IllegalStateException("createNote returned null"));
                        messages.send(sender, "command.database-error");
                        return;
                    }

                    messages.send(sender, "command.add-success", Map.of(
                            "id", String.valueOf(created.getId()),
                            "player", targetName
                    ));
                    plugin.audit().logNoteCreated(created);
                    plugin.discord().notifyNoteCreated(created);
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
