package de.codingplugs.playernotes.database;

import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.model.PlayerNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NoteRepository {

    CompletableFuture<PlayerNote> createNote(PlayerNote note);

    CompletableFuture<Optional<PlayerNote>> findById(long id);

    CompletableFuture<List<PlayerNote>> findByTarget(UUID targetUuid, boolean includeArchived);

    CompletableFuture<Boolean> archiveNote(long id);

    CompletableFuture<Boolean> deleteNote(long id);

    CompletableFuture<Integer> countActiveNotes(UUID targetUuid);

    CompletableFuture<Integer> countCriticalNotes(UUID targetUuid);

    CompletableFuture<Integer> countActiveNotesAtOrAbovePriority(UUID targetUuid, NotePriority minimumPriority);
}
