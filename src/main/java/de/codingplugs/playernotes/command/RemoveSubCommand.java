package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.model.PlayerNote;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RemoveSubCommand implements SubCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public RemoveSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "remove";
    }

    @Override
    public String permission() {
        return Permissions.REMOVE;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command.remove-usage");
            return true;
        }

        Long noteId = CommandSupport.parseNoteId(args[1]).orElse(null);
        if (noteId == null) {
            messages.send(sender, "command.invalid-id");
            return true;
        }

        CommandSupport.StaffIdentity staff = CommandSupport.staffIdentity(sender);

        plugin.notes().findById(noteId).thenCompose(optionalNote -> {
            if (optionalNote == null || optionalNote.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }

            PlayerNote note = optionalNote.get();
            return plugin.notes().deleteNote(noteId).thenApply(removed -> {
                if (removed != null && removed) {
                    plugin.audit().logNoteDeleted(note, staff.uuid(), staff.name());
                }
                return removed;
            });
        }).whenComplete((removed, error) ->
                CommandSupport.deliverFeedback(plugin, sender, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to remove note #" + noteId, error);
                        messages.send(sender, "command.database-error");
                        return;
                    }

                    if (removed == null || !removed) {
                        messages.send(sender, "command.note-not-found", Map.of("id", String.valueOf(noteId)));
                        return;
                    }

                    messages.send(sender, "command.remove-success", Map.of("id", String.valueOf(noteId)));
                    plugin.discord().notifyNoteDeleted(noteId, staff.name());
                }));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
