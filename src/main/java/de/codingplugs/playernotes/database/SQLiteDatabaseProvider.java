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

    public static final String DEFAULT_DATABASE_FILE = "playernotes.db";

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

        File databaseFile = new File(plugin.getDataFolder(), databaseFileName());
        String jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

        connection = DriverManager.getConnection(jdbcUrl);
        connection.setAutoCommit(true);

        try (Statement statement = connection.createStatement()) {
            statement.execute(DatabaseSchema.SQLITE_CREATE_TABLE);
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

    private String databaseFileName() {
        String configured = plugin.configManager().config().getString("storage.sqlite.file", DEFAULT_DATABASE_FILE);
        if (configured == null || configured.isBlank()) {
            return DEFAULT_DATABASE_FILE;
        }

        return configured.trim();
    }
}
