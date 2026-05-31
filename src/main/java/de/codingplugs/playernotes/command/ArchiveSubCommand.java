package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class ArchiveSubCommand implements SubCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public ArchiveSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "archive";
    }

    @Override
    public String permission() {
        return Permissions.ARCHIVE;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command.archive-usage");
            return true;
        }

        Long noteId = CommandSupport.parseNoteId(args[1]).orElse(null);
        if (noteId == null) {
            messages.send(sender, "command.invalid-id");
            return true;
        }

        CommandSupport.StaffIdentity staff = CommandSupport.staffIdentity(sender);

        plugin.notes().archiveNote(noteId).whenComplete((archived, error) ->
                CommandSupport.deliverFeedback(plugin, sender, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to archive note #" + noteId, error);
                        messages.send(sender, "command.database-error");
                        return;
                    }

                    if (archived == null || !archived) {
                        messages.send(sender, "command.note-not-found", Map.of("id", String.valueOf(noteId)));
                        return;
                    }

                    messages.send(sender, "command.archive-success", Map.of("id", String.valueOf(noteId)));
                    plugin.audit().logNoteArchived(noteId, staff.uuid(), staff.name());
                    plugin.discord().notifyNoteArchived(noteId, staff.name());
                }));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
