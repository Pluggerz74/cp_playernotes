package de.codingplugs.playernotes.util;

import de.codingplugs.playernotes.model.PlayerNote;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class NoteFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private NoteFormatter() {
    }

    public static String formatSummaryLine(PlayerNote note) {
        return "#" + note.getId()
                + " <gray>[</gray><white>" + note.getType().name() + "</white><gray>]</gray>"
                + " <gray>[</gray><white>" + note.getPriority().name() + "</white><gray>]</gray>"
                + " <white>" + note.getContent() + "</white>"
                + " <dark_gray>(" + DATE_TIME.format(note.getCreatedAt()) + " by " + note.getStaffName() + ")</dark_gray>";
    }

    public static String formatListLine(PlayerNote note) {
        return "#" + note.getId()
                + " <gray>|</gray> <white>" + note.getType().name() + "</white>"
                + " <gray>|</gray> <white>" + note.getPriority().name() + "</white>"
                + " <gray>|</gray> <white>" + note.getContent() + "</white>"
                + " <dark_gray>| " + DATE_TIME.format(note.getCreatedAt()) + " | " + note.getStaffName() + "</dark_gray>";
    }
}
