package de.codingplugs.playernotes.database;

import de.codingplugs.playernotes.model.AuditAction;
import de.codingplugs.playernotes.model.AuditLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SqlAuditLogRepository implements AuditLogRepository {

    private static final String INSERT = """
            INSERT INTO player_notes_audit (
                action, note_id, target_uuid, target_name,
                staff_uuid, staff_name, details, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_TARGET = """
            SELECT id, action, note_id, target_uuid, target_name,
                   staff_uuid, staff_name, details, created_at
            FROM player_notes_audit
            WHERE target_uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

    private static final String SELECT_BY_NOTE = """
            SELECT id, action, note_id, target_uuid, target_name,
                   staff_uuid, staff_name, details, created_at
            FROM player_notes_audit
            WHERE note_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

    private final DatabaseProvider databaseProvider;

    public SqlAuditLogRepository(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    @Override
    public CompletableFuture<AuditLogEntry> create(AuditLogEntry entry) {
        return supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, entry.getAction().name());
                statement.setLong(2, entry.getNoteId());
                statement.setString(3, entry.getTargetUuid().toString());
                statement.setString(4, entry.getTargetName());
                statement.setString(5, entry.getStaffUuid().toString());
                statement.setString(6, entry.getStaffName());
                statement.setString(7, entry.getDetails());
                statement.setString(8, entry.getCreatedAt().toString());

                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("Audit insert did not return a generated id.");
                    }

                    long id = generatedKeys.getLong(1);
                    return new AuditLogEntry(
                            id,
                            entry.getAction(),
                            entry.getNoteId(),
                            entry.getTargetUuid(),
                            entry.getTargetName(),
                            entry.getStaffUuid(),
                            entry.getStaffName(),
                            entry.getDetails(),
                            entry.getCreatedAt()
                    );
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<AuditLogEntry>> findByTarget(UUID targetUuid, int limit) {
        return findWithLimit(SELECT_BY_TARGET, targetUuid.toString(), limit);
    }

    @Override
    public CompletableFuture<List<AuditLogEntry>> findByNoteId(long noteId, int limit) {
        return supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_NOTE)) {
                statement.setLong(1, noteId);
                statement.setInt(2, Math.max(1, limit));

                try (ResultSet resultSet = statement.executeQuery()) {
                    return mapRows(resultSet);
                }
            }
        });
    }

    private CompletableFuture<List<AuditLogEntry>> findWithLimit(String query, String targetUuid, int limit) {
        return supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, targetUuid);
                statement.setInt(2, Math.max(1, limit));

                try (ResultSet resultSet = statement.executeQuery()) {
                    return mapRows(resultSet);
                }
            }
        });
    }

    private static List<AuditLogEntry> mapRows(ResultSet resultSet) throws SQLException {
        List<AuditLogEntry> entries = new ArrayList<>();
        while (resultSet.next()) {
            entries.add(mapRow(resultSet));
        }
        return entries;
    }

    private static AuditLogEntry mapRow(ResultSet resultSet) throws SQLException {
        return new AuditLogEntry(
                resultSet.getLong("id"),
                AuditAction.valueOf(resultSet.getString("action")),
                resultSet.getLong("note_id"),
                UUID.fromString(resultSet.getString("target_uuid")),
                resultSet.getString("target_name"),
                UUID.fromString(resultSet.getString("staff_uuid")),
                resultSet.getString("staff_name"),
                resultSet.getString("details"),
                Instant.parse(resultSet.getString("created_at"))
        );
    }

    private <T> CompletableFuture<T> supplyAsync(SqlFunction<T> function) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return withConnection(function);
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, databaseProvider.executor());
    }

    private <T> T withConnection(SqlFunction<T> function) throws SQLException {
        Connection connection = databaseProvider.connection();
        try {
            return function.apply(connection);
        } finally {
            if (databaseProvider.usesConnectionPool()) {
                connection.close();
            }
        }
    }

    @FunctionalInterface
    private interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
