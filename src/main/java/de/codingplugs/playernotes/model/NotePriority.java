package de.codingplugs.playernotes.model;

public enum NotePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL;

    public static NotePriority fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return HIGH;
        }

        try {
            return NotePriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return HIGH;
        }
    }

    public boolean isAtLeast(NotePriority minimum) {
        return this.ordinal() >= minimum.ordinal();
    }
}
