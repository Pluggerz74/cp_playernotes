package de.codingplugs.playernotes.service;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.database.AuditLogRepository;
import de.codingplugs.playernotes.model.AuditAction;
import de.codingplugs.playernotes.model.AuditLogEntry;
import de.codingplugs.playernotes.model.PlayerNote;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public final class AuditLogService {

    private final PlayerNotesPlugin plugin;
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(PlayerNotesPlugin plugin, AuditLogRepository auditLogRepository) {
        this.plugin = plugin;
        this.auditLogRepository = auditLogRepository;
    }

    public void logNoteCreated(PlayerNote note) {
        logNoteCreated(note, note.getStaffUuid(), note.getStaffName(), null);
    }

    public void logNoteCreated(PlayerNote note, UUID staffUuid, String staffName) {
        logNoteCreated(note, staffUuid, staffName, null);
    }

    public void logNoteCreated(PlayerNote note, UUID staffUuid, String staffName, String details) {
        write(new AuditLogEntry(
                0L,
                AuditAction.NOTE_CREATED,
                note.getId(),
                note.getTargetUuid(),
                note.getTargetName(),
                staffUuid,
                staffName,
                details == null ? "" : details,
                Instant.now()
        ));
    }

    public void logNoteEdited(long noteId, UUID staffUuid, String staffName) {
        logNoteEdited(noteId, staffUuid, staffName, "");
    }

    public void logNoteEdited(long noteId, UUID staffUuid, String staffName, String details) {
        plugin.notes().findById(noteId).whenComplete((optionalNote, error) -> {
            if (error != null || optionalNote == null || optionalNote.isEmpty()) {
                if (error != null) {
                    plugin.getLogger().log(Level.WARNING, "Failed to resolve note #" + noteId + " for audit log", error);
                }
                return;
            }

            PlayerNote note = optionalNote.get();
            write(new AuditLogEntry(
                    0L,
                    AuditAction.NOTE_EDITED,
                    noteId,
                    note.getTargetUuid(),
                    note.getTargetName(),
                    staffUuid,
                    staffName,
                    truncateDetails(details),
                    Instant.now()
            ));
        });
    }

    public void logNoteArchived(PlayerNote note, UUID staffUuid, String staffName) {
        write(new AuditLogEntry(
                0L,
                AuditAction.NOTE_ARCHIVED,
                note.getId(),
                note.getTargetUuid(),
                note.getTargetName(),
                staffUuid,
                staffName,
                "",
                Instant.now()
        ));
    }

    public void logNoteArchived(long noteId, UUID staffUuid, String staffName) {
        plugin.notes().findById(noteId).whenComplete((optionalNote, error) -> {
            if (error != null || optionalNote == null || optionalNote.isEmpty()) {
                if (error != null) {
                    plugin.getLogger().log(Level.WARNING, "Failed to resolve note #" + noteId + " for audit log", error);
                }
                return;
            }

            logNoteArchived(optionalNote.get(), staffUuid, staffName);
        });
    }

    public void logNoteDeleted(PlayerNote note, UUID staffUuid, String staffName) {
        write(new AuditLogEntry(
                0L,
                AuditAction.NOTE_DELETED,
                note.getId(),
                note.getTargetUuid(),
                note.getTargetName(),
                staffUuid,
                staffName,
                "",
                Instant.now()
        ));
    }

    public boolean isEnabled() {
        return plugin.configManager().config().getBoolean("audit.enabled", true);
    }

    public int maxHistoryResults() {
        return plugin.configManager().config().getInt("audit.max-history-command-results", 10);
    }

    private static String truncateDetails(String details) {
        if (details == null || details.isBlank()) {
            return "";
        }

        String trimmed = details.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }

        return trimmed.substring(0, 497) + "...";
    }

    private void write(AuditLogEntry entry) {
        if (!isEnabled()) {
            return;
        }

        auditLogRepository.create(entry).whenComplete((created, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to write audit log entry for note #" + entry.getNoteId(), error);
            }
        });
    }
}
