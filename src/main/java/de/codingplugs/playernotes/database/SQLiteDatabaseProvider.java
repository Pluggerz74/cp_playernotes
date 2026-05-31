package de.codingplugs.playernotes.database;

import de.codingplugs.playernotes.PlayerNotesPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SQLiteDatabaseProvider implements DatabaseProvider {

    public static final String DATABASE_FILE = "playernotes.db";

    private static final String CREATE_TABLE = """
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

    private final PlayerNotesPlugin plugin;
    private final Logger logger;

    private Connection connection;
    private ExecutorService executor;

    public SQLiteDatabaseProvider(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void initialize() throws SQLException {
        if (connection != null) {
            return;
        }

        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new SQLException("Could not create plugin data folder.");
        }

        File databaseFile = new File(plugin.getDataFolder(), DATABASE_FILE);
        String jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

        connection = DriverManager.getConnection(jdbcUrl);
        connection.setAutoCommit(true);

        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        }

        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cp_playernotes-db");
            thread.setDaemon(true);
            return thread;
        });

        logger.info("SQLite database initialized at " + databaseFile.getAbsolutePath());
    }

    @Override
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                logger.log(Level.WARNING, "Failed to close SQLite connection", exception);
            }
            connection = null;
        }
    }

    @Override
    public Connection connection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database is not initialized.");
        }
        return connection;
    }

    @Override
    public ExecutorService executor() {
        if (executor == null) {
            throw new IllegalStateException("Database executor is not initialized.");
        }
        return executor;
    }
}
