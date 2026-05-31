package de.codingplugs.playernotes.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PlayerNote {

    private final long id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID staffUuid;
    private final String staffName;
    private NoteType type;
    private NotePriority priority;
    private String content;
    private final Instant createdAt;
    private Instant updatedAt;
    private boolean archived;

    public PlayerNote(
            long id,
            UUID targetUuid,
            String targetName,
            UUID staffUuid,
            String staffName,
            NoteType type,
            NotePriority priority,
            String content,
            Instant createdAt,
            Instant updatedAt,
            boolean archived
    ) {
        this.id = id;
        this.targetUuid = Objects.requireNonNull(targetUuid, "targetUuid must not be null");
        this.targetName = requireNonBlank(targetName, "targetName must not be blank");
        this.staffUuid = Objects.requireNonNull(staffUuid, "staffUuid must not be null");
        this.staffName = requireNonBlank(staffName, "staffName must not be blank");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.content = requireNonBlank(content, "content must not be blank");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.archived = archived;
    }

    public long getId() {
        return id;
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

    public NoteType getType() {
        return type;
    }

    public NotePriority getPriority() {
        return priority;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isCritical() {
        return priority == NotePriority.CRITICAL;
    }

    public boolean isActive() {
        return !archived;
    }

    public void archive() {
        archived = true;
        touch();
    }

    public void unarchive() {
        archived = false;
        touch();
    }

    public void updateContent(String newContent) {
        this.content = requireNonBlank(newContent, "content must not be blank");
        touch();
    }

    public void updatePriority(NotePriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        touch();
    }

    public void updateType(NoteType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
