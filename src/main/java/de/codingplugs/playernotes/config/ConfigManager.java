package de.codingplugs.playernotes.config;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

public final class ConfigManager {

    public static final String CONFIG_FILE = "config.yml";
    public static final String MESSAGES_FILE = "messages.yml";
    public static final String GUI_FILE = "gui.yml";

    private static final List<String> ALL_FILES = List.of(
            CONFIG_FILE,
            MESSAGES_FILE,
            GUI_FILE
    );

    private final PlayerNotesPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration gui;

    public ConfigManager(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean load() {
        try {
            ensureDataFolder();
            installDefaultConfigs();
            reload();
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration", exception);
            return false;
        }
    }

    public void reload() {
        config = loadConfig(CONFIG_FILE);
        messages = loadConfig(MESSAGES_FILE);
        gui = loadConfig(GUI_FILE);
    }

    public FileConfiguration config() {
        return config;
    }

    public FileConfiguration messages() {
        return messages;
    }

    public FileConfiguration gui() {
        return gui;
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }
    }

    private void installDefaultConfigs() {
        for (String fileName : ALL_FILES) {
            File target = new File(plugin.getDataFolder(), fileName);
            if (!target.exists()) {
                plugin.saveResource(fileName, false);
            }
        }
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        applyDefaults(yaml, fileName);
        return yaml;
    }

    private void applyDefaults(YamlConfiguration target, String fileName) {
        InputStream stream = plugin.getResource(fileName);
        if (stream == null) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            target.setDefaults(defaults);
            target.options().copyDefaults(true);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not apply defaults for " + fileName, exception);
        }
    }
}
