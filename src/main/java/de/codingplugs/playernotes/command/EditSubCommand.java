package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class EditSubCommand implements SubCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public EditSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "edit";
    }

    @Override
    public String permission() {
        return Permissions.EDIT;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "command.edit-usage");
            return true;
        }

        Long noteId = CommandSupport.parseNoteId(args[1]).orElse(null);
        if (noteId == null) {
            messages.send(sender, "command.invalid-id");
            return true;
        }

        String content = CommandSupport.joinArgs(args, 2);
        if (content.isBlank()) {
            messages.send(sender, "command.edit-usage");
            return true;
        }

        CommandSupport.StaffIdentity staff = CommandSupport.staffIdentity(sender);

        plugin.notes().updateNoteContent(noteId, content).whenComplete((updated, error) ->
                CommandSupport.deliverFeedback(plugin, sender, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to edit note #" + noteId, error);
                        messages.send(sender, "command.database-error");
                        return;
                    }

                    if (updated == null || !updated) {
                        messages.send(sender, "command.note-not-found", Map.of("id", String.valueOf(noteId)));
                        return;
                    }

                    messages.send(sender, "command.edit-success", Map.of("id", String.valueOf(noteId)));
                    plugin.audit().logNoteEdited(noteId, staff.uuid(), staff.name(), content);
                }));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
