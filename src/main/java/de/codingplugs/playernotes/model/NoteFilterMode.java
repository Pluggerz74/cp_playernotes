package de.codingplugs.playernotes.model;

public enum NoteFilterMode {
    ACTIVE,
    ARCHIVED,
    ALL;

    public NoteFilterMode next() {
        return switch (this) {
            case ACTIVE -> ARCHIVED;
            case ARCHIVED -> ALL;
            case ALL -> ACTIVE;
        };
    }

    public String sqlSuffix() {
        return switch (this) {
            case ACTIVE -> " AND archived = 0";
            case ARCHIVED -> " AND archived = 1";
            case ALL -> "";
        };
    }
}
