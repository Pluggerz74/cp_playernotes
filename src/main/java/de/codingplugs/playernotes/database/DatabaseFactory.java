package de.codingplugs.playernotes.database;

import de.codingplugs.playernotes.PlayerNotesPlugin;

import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Logger;

public final class DatabaseFactory {

    private DatabaseFactory() {
    }

    public static DatabaseProvider create(PlayerNotesPlugin plugin) {
        String storageType = plugin.configManager().config()
                .getString("storage.type", "sqlite")
                .trim()
                .toLowerCase(Locale.ROOT);

        Logger logger = plugin.getLogger();

        return switch (storageType) {
            case "mysql", "mariadb" -> {
                logger.info("Using MySQL/MariaDB storage backend.");
                yield new MySQLDatabaseProvider(plugin);
            }
            case "sqlite" -> {
                logger.info("Using SQLite storage backend.");
                yield new SQLiteDatabaseProvider(plugin);
            }
            default -> {
                logger.warning("Unknown storage.type '" + storageType + "'. Falling back to SQLite.");
                yield new SQLiteDatabaseProvider(plugin);
            }
        };
    }

    public static String storageTypeLabel(PlayerNotesPlugin plugin) {
        return plugin.configManager().config().getString("storage.type", "sqlite");
    }
}
