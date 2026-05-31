package de.codingplugs.playernotes.database;

public final class DatabaseSchema {

    public static final String SQLITE_CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS player_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                target_uuid TEXT NOT NULL,
                target_name TEXT NOT NULL,
                staff_uuid TEXT NOT NULL,
                staff_name TEXT NOT NULL,
                type TEXT NOT NULL,
                priority TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                archived INTEGER NOT NULL DEFAULT 0
            )
            """;

    public static final String MYSQL_CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS player_notes (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                target_uuid VARCHAR(36) NOT NULL,
                target_name VARCHAR(64) NOT NULL,
                staff_uuid VARCHAR(36) NOT NULL,
                staff_name VARCHAR(64) NOT NULL,
                type VARCHAR(32) NOT NULL,
                priority VARCHAR(32) NOT NULL,
                content TEXT NOT NULL,
                created_at VARCHAR(32) NOT NULL,
                updated_at VARCHAR(32) NOT NULL,
                archived TINYINT(1) NOT NULL DEFAULT 0
            )
            """;

    public static final String SQLITE_CREATE_AUDIT_TABLE = """
            CREATE TABLE IF NOT EXISTS player_notes_audit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action TEXT NOT NULL,
                note_id INTEGER NOT NULL,
                target_uuid TEXT NOT NULL,
                target_name TEXT NOT NULL,
                staff_uuid TEXT NOT NULL,
                staff_name TEXT NOT NULL,
                details TEXT,
                created_at TEXT NOT NULL
            )
            """;

    public static final String MYSQL_CREATE_AUDIT_TABLE = """
            CREATE TABLE IF NOT EXISTS player_notes_audit (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                action TEXT NOT NULL,
                note_id BIGINT NOT NULL,
                target_uuid VARCHAR(36) NOT NULL,
                target_name VARCHAR(64) NOT NULL,
                staff_uuid VARCHAR(36) NOT NULL,
                staff_name VARCHAR(64) NOT NULL,
                details TEXT,
                created_at VARCHAR(32) NOT NULL
            )
            """;

    private DatabaseSchema() {
    }
}
