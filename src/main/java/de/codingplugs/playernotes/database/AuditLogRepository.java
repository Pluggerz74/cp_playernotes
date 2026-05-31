package de.codingplugs.playernotes.database;

import de.codingplugs.playernotes.model.AuditLogEntry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AuditLogRepository {

    CompletableFuture<AuditLogEntry> create(AuditLogEntry entry);

    CompletableFuture<List<AuditLogEntry>> findByTarget(UUID targetUuid, int limit);

    CompletableFuture<List<AuditLogEntry>> findByNoteId(long noteId, int limit);
}
