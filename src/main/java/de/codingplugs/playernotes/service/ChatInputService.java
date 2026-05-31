package de.codingplugs.playernotes.service;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.gui.GuiManager;
import de.codingplugs.playernotes.model.NoteFilterMode;
import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.model.NoteType;
import de.codingplugs.playernotes.model.PlayerNote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputService {

    public static final long TIMEOUT_TICKS = 60L * 20L;

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;
    private final GuiManager guiManager;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputService(PlayerNotesPlugin plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.guiManager = guiManager;
    }

    public void startInput(
            Player staff,
            UUID targetUuid,
            String targetName,
            NoteType type,
            NotePriority priority,
            int page,
            NoteFilterMode filterMode
    ) {
        UUID staffUuid = staff.getUniqueId();
        cancelInput(staffUuid);

        PendingInput pending = PendingInput.forCreate(
                targetUuid,
                targetName,
                type,
                priority,
                page,
                filterMode,
                Instant.now()
        );
        pendingInputs.put(staffUuid, pending);

        messages.send(staff, "chat-input.start", Map.of(
                "type", type.name(),
                "priority", priority.name()
        ));
        scheduleTimeout(staffUuid, pending.createdAt());
    }

    public void startEditInput(
            Player staff,
            long noteId,
            UUID targetUuid,
            String targetName,
            int page,
            NoteFilterMode filterMode
    ) {
        UUID staffUuid = staff.getUniqueId();
        cancelInput(staffUuid);

        PendingInput pending = PendingInput.forEdit(
                noteId,
                targetUuid,
                targetName,
                page,
                filterMode,
                Instant.now()
        );
        pendingInputs.put(staffUuid, pending);

        messages.send(staff, "chat-input.edit-start");
        scheduleTimeout(staffUuid, pending.createdAt());
    }

    public boolean hasPendingInput(UUID staffUuid) {
        return getPendingInput(staffUuid).isPresent();
    }

    public Optional<PendingInput> getPendingInput(UUID staffUuid) {
        PendingInput pending = pendingInputs.get(staffUuid);
        if (pending == null) {
            return Optional.empty();
        }

        if (pending.isExpired()) {
            pendingInputs.remove(staffUuid);
            return Optional.empty();
        }

        return Optional.of(pending);
    }

    public void handleMessage(Player staff, String message) {
        Optional<PendingInput> optionalPending = getPendingInput(staff.getUniqueId());
        if (optionalPending.isEmpty()) {
            return;
        }

        PendingInput pending = optionalPending.get();
        String trimmed = message.trim();

        if (trimmed.equalsIgnoreCase("cancel")) {
            cancelInput(staff.getUniqueId());
            messages.send(staff, "chat-input.cancelled");
            return;
        }

        if (trimmed.isBlank()) {
            return;
        }

        pendingInputs.remove(staff.getUniqueId());

        if (pending.isEdit()) {
            updateNote(staff, pending, trimmed);
        } else {
            saveNote(staff, pending, trimmed);
        }
    }

    public void cancelInput(UUID staffUuid) {
        pendingInputs.remove(staffUuid);
    }

    public void shutdown() {
        pendingInputs.clear();
    }

    private void saveNote(Player staff, PendingInput pending, String content) {
        Instant now = Instant.now();
        PlayerNote note = new PlayerNote(
                0L,
                pending.targetUuid(),
                pending.targetName(),
                staff.getUniqueId(),
                staff.getName(),
                pending.type(),
                pending.priority(),
                content,
                now,
                now,
                false
        );

        plugin.notes().createNote(note).whenComplete((created, error) ->
                CommandSupport.deliverFeedback(plugin, staff, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to save GUI note for " + pending.targetName(), error);
                        messages.send(staff, "chat-input.error");
                        return;
                    }

                    if (created == null) {
                        plugin.logSevere(
                                "Failed to save GUI note for " + pending.targetName(),
                                new IllegalStateException("createNote returned null")
                        );
                        messages.send(staff, "chat-input.error");
                        return;
                    }

                    messages.send(staff, "chat-input.saved", Map.of(
                            "id", String.valueOf(created.getId()),
                            "player", pending.targetName()
                    ));

                    plugin.audit().logNoteCreated(created);
                    plugin.discord().notifyNoteCreated(created);

                    guiManager.reopenPlayerNotes(
                            staff,
                            pending.targetUuid(),
                            pending.targetName(),
                            pending.page(),
                            pending.filterMode()
                    );
                }));
    }

    private void updateNote(Player staff, PendingInput pending, String content) {
        long noteId = pending.editNoteId();

        plugin.notes().updateNoteContent(noteId, content).whenComplete((updated, error) ->
                CommandSupport.deliverFeedback(plugin, staff, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to edit note #" + noteId, error);
                        messages.send(staff, "chat-input.error");
                        return;
                    }

                    if (updated == null || !updated) {
                        messages.send(staff, "command.note-not-found", Map.of("id", String.valueOf(noteId)));
                        guiManager.reopenPlayerNotes(
                                staff,
                                pending.targetUuid(),
                                pending.targetName(),
                                pending.page(),
                                pending.filterMode()
                        );
                        return;
                    }

                    messages.send(staff, "chat-input.edit-saved", Map.of("id", String.valueOf(noteId)));
                    plugin.audit().logNoteEdited(noteId, staff.getUniqueId(), staff.getName(), content);

                    guiManager.reopenAfterEdit(
                            staff,
                            noteId,
                            pending.targetUuid(),
                            pending.targetName(),
                            pending.page(),
                            pending.filterMode()
                    );
                }));
    }

    private void scheduleTimeout(UUID staffUuid, Instant startedAt) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingInput pending = pendingInputs.get(staffUuid);
            if (pending == null || !pending.createdAt().equals(startedAt)) {
                return;
            }

            pendingInputs.remove(staffUuid);

            Player staff = Bukkit.getPlayer(staffUuid);
            if (staff != null && staff.isOnline()) {
                messages.send(staff, "chat-input.timeout");
            }
        }, TIMEOUT_TICKS);
    }

    public record PendingInput(
            UUID targetUuid,
            String targetName,
            NoteType type,
            NotePriority priority,
            Long editNoteId,
            int page,
            NoteFilterMode filterMode,
            Instant createdAt
    ) {

        private static final long TIMEOUT_SECONDS = 60;

        public static PendingInput forCreate(
                UUID targetUuid,
                String targetName,
                NoteType type,
                NotePriority priority,
                int page,
                NoteFilterMode filterMode,
                Instant createdAt
        ) {
            return new PendingInput(targetUuid, targetName, type, priority, null, page, filterMode, createdAt);
        }

        public static PendingInput forEdit(
                long noteId,
                UUID targetUuid,
                String targetName,
                int page,
                NoteFilterMode filterMode,
                Instant createdAt
        ) {
            return new PendingInput(targetUuid, targetName, null, null, noteId, page, filterMode, createdAt);
        }

        public boolean isEdit() {
            return editNoteId != null;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(TIMEOUT_SECONDS));
        }
    }
}
