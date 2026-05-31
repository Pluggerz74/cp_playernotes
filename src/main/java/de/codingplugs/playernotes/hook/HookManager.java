package de.codingplugs.playernotes.hook;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public final class HookManager {

    private final PlayerNotesPlugin plugin;
    private PlaceholderApiHook placeholderApiHook;

    public HookManager(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerHooks() {
        registerPlaceholderApi();
    }

    public void shutdown() {
        unregisterPlaceholderApi();
    }

    public void reload() {
        unregisterPlaceholderApi();
        registerPlaceholderApi();
    }

    private void registerPlaceholderApi() {
        if (!plugin.configManager().config().getBoolean("hooks.placeholderapi", true)) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        placeholderApiHook = new PlaceholderApiHook(plugin);
        if (placeholderApiHook.register()) {
            plugin.getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            plugin.getLogger().log(Level.WARNING, "Failed to register PlaceholderAPI expansion.");
            placeholderApiHook = null;
        }
    }

    private void unregisterPlaceholderApi() {
        if (placeholderApiHook == null) {
            return;
        }

        placeholderApiHook.clearCache();
        placeholderApiHook.unregister();
        placeholderApiHook = null;
    }

    public boolean isPlaceholderApiInstalled() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isPlaceholderApiRegistered() {
        return placeholderApiHook != null;
    }
}
