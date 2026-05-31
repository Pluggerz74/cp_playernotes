package de.codingplugs.playernotes.database;

import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.model.NoteType;
import de.codingplugs.playernotes.model.PlayerNote;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SqlNoteRepository implements NoteRepository {

    private static final String INSERT_NOTE = """
            INSERT INTO player_notes (
                target_uuid, target_name, staff_uuid, staff_name,
                type, priority, content, created_at, updated_at, archived
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, target_uuid, target_name, staff_uuid, staff_name,
                   type, priority, content, created_at, updated_at, archived
            FROM player_notes
            WHERE id = ?
            """;

    private static final String SELECT_BY_TARGET = """
            SELECT id, target_uuid, target_name, staff_uuid, staff_name,
                   type, priority, content, created_at, updated_at, archived
            FROM player_notes
            WHERE target_uuid = ?
            """;

    private static final String ARCHIVE_NOTE = """
            UPDATE player_notes
            SET archived = 1, updated_at = ?
            WHERE id = ? AND archived = 0
            """;

    private static final String DELETE_NOTE = """
            DELETE FROM player_notes
            WHERE id = ?
            """;

    private static final String COUNT_ACTIVE = """
            SELECT COUNT(*)
            FROM player_notes
            WHERE target_uuid = ? AND archived = 0
            """;

    private static final String COUNT_CRITICAL = """
            SELECT COUNT(*)
            FROM player_notes
            WHERE target_uuid = ? AND archived = 0 AND priority = ?
            """;

    private final DatabaseProvider databaseProvider;

    public SqlNoteRepository(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    @Override
    public CompletableFuture<PlayerNote> createNote(PlayerNote note) {
        return supplyAsync(() -> {
            Connection connection = databaseProvider.connection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_NOTE, Statement.RETURN_GENERATED_KEYS)) {
                bindNoteForInsert(statement, note);

                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("Insert did not return a generated id.");
                    }

                    long id = generatedKeys.getLong(1);
                    return new PlayerNote(
                            id,
                            note.getTargetUuid(),
                            note.getTargetName(),
                            note.getStaffUuid(),
                            note.getStaffName(),
                            note.getType(),
                            note.getPriority(),
                            note.getContent(),
                            note.getCreatedAt(),
                            note.getUpdatedAt(),
                            note.isArchived()
                    );
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<PlayerNote>> findById(long id) {
        return supplyAsync(() -> {
            Connection connection = databaseProvider.connection();
            try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
                statement.setLong(1, id);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(resultSet));
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<PlayerNote>> findByTarget(UUID targetUuid, boolean includeArchived) {
        return supplyAsync(() -> {
            String query = includeArchived
                    ? SELECT_BY_TARGET + " ORDER BY created_at DESC"
                    : SELECT_BY_TARGET + " AND archived = 0 ORDER BY created_at DESC";

            Connection connection = databaseProvider.connection();
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, targetUuid.toString());

                try (ResultSet resultSet = statement.executeQuery()) {
                    List<PlayerNote> notes = new ArrayList<>();
                    while (resultSet.next()) {
                        notes.add(mapRow(resultSet));
                    }
                    return notes;
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> archiveNote(long id) {
        return supplyAsync(() -> {
            Connection connection = databaseProvider.connection();
            try (PreparedStatement statement = connection.prepareStatement(ARCHIVE_NOTE)) {
                statement.setString(1, Instant.now().toString());
                statement.setLong(2, id);
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteNote(long id) {
        return supplyAsync(() -> {
            Connection connection = databaseProvider.connection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_NOTE)) {
                statement.setLong(1, id);
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> countActiveNotes(UUID targetUuid) {
        return supplyAsync(() -> {
            Connection connection = databaseProvider.connection();
            try (PreparedStatement statement = connection.prepareStatement(COUNT_ACTIVE)) {
                statement.setString(1, targetUuid.toString());

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Integer> countCriticalNotes(UUID targetUuid) {
        return supplyAsync(() -> {
            Connection connection = databaseProvider.connection();
            try (PreparedStatement statement = connection.prepareStatement(COUNT_CRITICAL)) {
                statement.setString(1, targetUuid.toString());
                statement.setString(2, NotePriority.CRITICAL.name());

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        });
    }

    private <T> CompletableFuture<T> supplyAsync(SqlSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, databaseProvider.executor());
    }

    private static void bindNoteForInsert(PreparedStatement statement, PlayerNote note) throws SQLException {
        statement.setString(1, note.getTargetUuid().toString());
        statement.setString(2, note.getTargetName());
        statement.setString(3, note.getStaffUuid().toString());
        statement.setString(4, note.getStaffName());
        statement.setString(5, note.getType().name());
        statement.setString(6, note.getPriority().name());
        statement.setString(7, note.getContent());
        statement.setString(8, note.getCreatedAt().toString());
        statement.setString(9, note.getUpdatedAt().toString());
        statement.setInt(10, note.isArchived() ? 1 : 0);
    }

    private static PlayerNote mapRow(ResultSet resultSet) throws SQLException {
        return new PlayerNote(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("target_uuid")),
                resultSet.getString("target_name"),
                UUID.fromString(resultSet.getString("staff_uuid")),
                resultSet.getString("staff_name"),
                NoteType.valueOf(resultSet.getString("type")),
                NotePriority.valueOf(resultSet.getString("priority")),
                resultSet.getString("content"),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at")),
                resultSet.getInt("archived") == 1
        );
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }
}
