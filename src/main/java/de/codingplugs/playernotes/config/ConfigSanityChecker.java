package de.codingplugs.playernotes.config;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.database.DatabaseFactory;
import de.codingplugs.playernotes.model.NotePriority;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigSanityChecker {

    private static final Set<String> KNOWN_STORAGE_TYPES = Set.of("sqlite", "mysql", "mariadb");

    private static final List<String> GUI_TITLE_PATHS = List.of(
            "notes-menu.title",
            "detail-menu.title",
            "type-select-menu.title",
            "priority-select-menu.title"
    );

    private final PlayerNotesPlugin plugin;

    public ConfigSanityChecker(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAndLogWarnings() {
        for (String warning : collectWarnings()) {
            plugin.getLogger().warning("[Config] " + warning);
        }
    }

    public List<String> collectWarnings() {
        List<String> warnings = new ArrayList<>();
        FileConfiguration config = plugin.configManager().config();
        FileConfiguration gui = plugin.configManager().gui();

        checkStorage(config, warnings);
        checkJoinAlerts(config, warnings);
        checkDiscord(config, warnings);
        checkPlaceholderApi(config, warnings);
        checkGuiTitles(gui, warnings);
        checkGuiMaterials(gui, warnings);

        return warnings;
    }

    private void checkStorage(FileConfiguration config, List<String> warnings) {
        String storageType = config.getString("storage.type", "sqlite");
        String normalized = storageType == null ? "" : storageType.trim().toLowerCase(Locale.ROOT);

        if (!KNOWN_STORAGE_TYPES.contains(normalized)) {
            warnings.add("Unknown storage.type '" + storageType + "'. Supported values: sqlite, mysql, mariadb.");
        }

        if ("sqlite".equals(normalized)) {
            String fileName = config.getString("storage.sqlite.file", "playernotes.db");
            if (fileName == null || fileName.isBlank()) {
                warnings.add("storage.sqlite.file is blank. Using default playernotes.db.");
            }
        }

        if ("mysql".equals(normalized) || "mariadb".equals(normalized)) {
            if (isBlank(config.getString("storage.mysql.host"))) {
                warnings.add("MySQL storage is enabled but storage.mysql.host is empty.");
            }
            if (isBlank(config.getString("storage.mysql.database"))) {
                warnings.add("MySQL storage is enabled but storage.mysql.database is empty.");
            }
            if (isBlank(config.getString("storage.mysql.username"))) {
                warnings.add("MySQL storage is enabled but storage.mysql.username is empty.");
            }
        }
    }

    private void checkJoinAlerts(FileConfiguration config, List<String> warnings) {
        String minimumPriority = config.getString("join-alerts.minimum-priority", "HIGH");
        if (!isValidPriority(minimumPriority)) {
            warnings.add("Invalid join-alerts.minimum-priority '" + minimumPriority
                    + "'. Valid values: LOW, NORMAL, HIGH, CRITICAL.");
        }

        String permission = config.getString("join-alerts.permission");
        if (permission == null || permission.isBlank()) {
            warnings.add("join-alerts.permission is blank.");
        }
    }

    private void checkDiscord(FileConfiguration config, List<String> warnings) {
        if (!config.getBoolean("discord.enabled", false)) {
            return;
        }

        String webhookUrl = config.getString("discord.webhook-url", "");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            warnings.add("Discord webhooks are enabled but discord.webhook-url is empty.");
        }
    }

    private void checkPlaceholderApi(FileConfiguration config, List<String> warnings) {
        if (!config.getBoolean("hooks.placeholderapi", true)) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            warnings.add("PlaceholderAPI hook is enabled in config but PlaceholderAPI is not installed.");
        }
    }

    private void checkGuiTitles(FileConfiguration gui, List<String> warnings) {
        for (String path : GUI_TITLE_PATHS) {
            String title = gui.getString(path);
            if (title == null || title.isBlank()) {
                warnings.add("GUI title missing or blank: " + path);
            }
        }
    }

    private void checkGuiMaterials(FileConfiguration gui, List<String> warnings) {
        ConfigurationSection materials = gui.getConfigurationSection("notes-menu.materials");
        if (materials == null) {
            return;
        }

        for (String key : materials.getKeys(false)) {
            String configured = materials.getString(key);
            if (configured == null || configured.isBlank()) {
                warnings.add("GUI material '" + key + "' is blank in notes-menu.materials.");
                continue;
            }

            if (Material.matchMaterial(configured) == null) {
                warnings.add("Invalid GUI material '" + configured + "' for notes-menu.materials." + key
                        + ". Fallback materials will be used.");
            }
        }
    }

    private static boolean isValidPriority(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            NotePriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
