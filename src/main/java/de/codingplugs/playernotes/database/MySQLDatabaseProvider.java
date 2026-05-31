package de.codingplugs.playernotes.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.codingplugs.playernotes.PlayerNotesPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class MySQLDatabaseProvider implements DatabaseProvider {

    private final PlayerNotesPlugin plugin;
    private final Logger logger;

    private HikariDataSource dataSource;
    private ExecutorService executor;

    public MySQLDatabaseProvider(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void initialize() throws SQLException {
        if (dataSource != null) {
            return;
        }

        FileConfiguration config = plugin.configManager().config();
        String host = config.getString("storage.mysql.host", "localhost");
        int port = config.getInt("storage.mysql.port", 3306);
        String database = config.getString("storage.mysql.database", "playernotes");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "");
        boolean useSsl = config.getBoolean("storage.mysql.use-ssl", false);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(buildJdbcUrl(host, port, database, useSsl));
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(config.getInt("storage.mysql.pool.maximum-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("storage.mysql.pool.minimum-idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("storage.mysql.pool.connection-timeout", 30_000L));
        hikariConfig.setIdleTimeout(config.getLong("storage.mysql.pool.idle-timeout", 600_000L));
        hikariConfig.setMaxLifetime(config.getLong("storage.mysql.pool.max-lifetime", 1_800_000L));
        hikariConfig.setPoolName("cp_playernotes-mysql");

        try {
            dataSource = new HikariDataSource(hikariConfig);

            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(DatabaseSchema.MYSQL_CREATE_TABLE);
            }
        } catch (SQLException exception) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }

            throw new SQLException(
                    "Failed to connect to MySQL at " + host + ":" + port + "/" + database
                            + ". Check host, credentials, and that the database exists.",
                    exception
            );
        }

        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cp_playernotes-db");
            thread.setDaemon(true);
            return thread;
        });

        logger.info("MySQL database connected at " + host + ":" + port + "/" + database);
    }

    @Override
    public void shutdown() {
        shutdownExecutor();

        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    @Override
    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database is not initialized.");
        }

        return dataSource.getConnection();
    }

    @Override
    public boolean usesConnectionPool() {
        return true;
    }

    @Override
    public ExecutorService executor() {
        if (executor == null) {
            throw new IllegalStateException("Database executor is not initialized.");
        }

        return executor;
    }

    private static String buildJdbcUrl(String host, int port, String database, boolean useSsl) {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSsl
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC";
    }

    private void shutdownExecutor() {
        if (executor == null) {
            return;
        }

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
}
