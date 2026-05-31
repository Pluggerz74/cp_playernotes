package de.codingplugs.playernotes.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuditLogEntry {

    private final long id;
    private final AuditAction action;
    private final long noteId;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID staffUuid;
    private final String staffName;
    private final String details;
    private final Instant createdAt;

    public AuditLogEntry(
            long id,
            AuditAction action,
            long noteId,
            UUID targetUuid,
            String targetName,
            UUID staffUuid,
            String staffName,
            String details,
            Instant createdAt
    ) {
        this.id = id;
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.noteId = noteId;
        this.targetUuid = Objects.requireNonNull(targetUuid, "targetUuid must not be null");
        this.targetName = requireNonBlank(targetName, "targetName must not be blank");
        this.staffUuid = Objects.requireNonNull(staffUuid, "staffUuid must not be null");
        this.staffName = requireNonBlank(staffName, "staffName must not be blank");
        this.details = details == null ? "" : details;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public long getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public long getNoteId() {
        return noteId;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getStaffUuid() {
        return staffUuid;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
