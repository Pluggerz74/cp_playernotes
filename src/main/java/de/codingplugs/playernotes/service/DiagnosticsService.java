package de.codingplugs.playernotes.service;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.hook.HookManager;
import de.codingplugs.playernotes.model.NotePriority;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DiagnosticsService {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public DiagnosticsService(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void sendDebugReport(CommandSender sender) {
        Map<String, String> placeholders = buildStaticPlaceholders(resolveDatabaseStatus());

        messages.sendLines(sender, "command.debug", placeholders);

        if (plugin.notes() == null) {
            messages.send(sender, "command.debug-notes", Map.of(
                    "total", messages.raw("command.debug-unavailable"),
                    "active", messages.raw("command.debug-unavailable")
            ));
            return;
        }

        CompletableFuture<Integer> totalNotes = plugin.notes().countAllNotes();
        CompletableFuture<Integer> activeNotes = plugin.notes().countAllActiveNotes();

        CompletableFuture.allOf(totalNotes, activeNotes).whenComplete((ignored, error) ->
                CommandSupport.deliverFeedback(plugin, sender, messages, () -> {
                    String unavailable = messages.raw("command.debug-unavailable");
                    String total = error != null ? unavailable : formatCountResult(totalNotes, unavailable);
                    String active = error != null ? unavailable : formatCountResult(activeNotes, unavailable);

                    messages.send(sender, "command.debug-notes", Map.of(
                            "total", total,
                            "active", active
                    ));
                }));
    }

    private Map<String, String> buildStaticPlaceholders(String databaseStatus) {
        FileConfiguration config = plugin.configManager().config();
        HookManager hooks = plugin.hooks();

        String storageType = normalizeStorageType(config.getString("storage.type", "sqlite"));
        String placeholderApi = formatPlaceholderApiStatus(hooks, config);
        String discord = formatDiscordStatus(config);
        String joinAlerts = formatJoinAlertsStatus(config);
        String audit = formatAuditStatus(config);

        return Map.ofEntries(
                Map.entry("plugin", plugin.getDescription().getName()),
                Map.entry("version", plugin.getDescription().getVersion()),
                Map.entry("server", Bukkit.getName() + " " + Bukkit.getVersion()),
                Map.entry("java", System.getProperty("java.version")),
                Map.entry("storage", storageType),
                Map.entry("database", databaseStatus),
                Map.entry("placeholderapi", placeholderApi),
                Map.entry("discord", discord),
                Map.entry("join_alerts", joinAlerts),
                Map.entry("audit", audit),
                Map.entry("gui_config", isGuiConfigLoaded() ? messages.raw("command.debug-yes") : messages.raw("command.debug-no")),
                Map.entry("messages_config", isMessagesConfigLoaded() ? messages.raw("command.debug-yes") : messages.raw("command.debug-no"))
        );
    }

    private String resolveDatabaseStatus() {
        if (plugin.databaseProvider() == null) {
            return messages.raw("command.debug-database-failed");
        }

        try {
            Connection connection = plugin.databaseProvider().connection();
            if (plugin.databaseProvider().usesConnectionPool()) {
                connection.close();
            }
            return messages.raw("command.debug-database-connected");
        } catch (SQLException exception) {
            return messages.raw("command.debug-database-failed");
        }
    }

    private static String normalizeStorageType(String storageType) {
        if (storageType == null || storageType.isBlank()) {
            return "sqlite";
        }

        return storageType.trim().toLowerCase(Locale.ROOT);
    }

    private String formatPlaceholderApiStatus(HookManager hooks, FileConfiguration config) {
        boolean installed = hooks != null && hooks.isPlaceholderApiInstalled();
        boolean hookEnabled = config.getBoolean("hooks.placeholderapi", true);
        boolean registered = hooks != null && hooks.isPlaceholderApiRegistered();

        return String.join(" / ",
                label("installed", installed),
                label("hook enabled", hookEnabled),
                label("registered", registered)
        );
    }

    private String formatDiscordStatus(FileConfiguration config) {
        boolean enabled = config.getBoolean("discord.enabled", false);
        boolean configured = !config.getString("discord.webhook-url", "").trim().isBlank();

        if (!enabled) {
            return messages.raw("command.debug-discord-disabled");
        }

        return configured
                ? messages.raw("command.debug-discord-enabled-configured")
                : messages.raw("command.debug-discord-enabled-unconfigured");
    }

    private String formatJoinAlertsStatus(FileConfiguration config) {
        boolean enabled = config.getBoolean("join-alerts.enabled", true);
        NotePriority minimumPriority = NotePriority.fromConfig(config.getString("join-alerts.minimum-priority", "HIGH"));
        String state = enabled
                ? messages.raw("command.debug-enabled")
                : messages.raw("command.debug-disabled");

        return state + " (" + minimumPriority.name() + "+)";
    }

    private String formatAuditStatus(FileConfiguration config) {
        boolean enabled = config.getBoolean("audit.enabled", true);
        return enabled
                ? messages.raw("command.debug-enabled")
                : messages.raw("command.debug-disabled");
    }

    private boolean isGuiConfigLoaded() {
        FileConfiguration gui = plugin.configManager().gui();
        return gui != null && gui.contains("notes-menu.title");
    }

    private boolean isMessagesConfigLoaded() {
        FileConfiguration messagesConfig = plugin.configManager().messages();
        return messagesConfig != null && messagesConfig.contains("prefix");
    }

    private String formatCountResult(CompletableFuture<Integer> future, String unavailable) {
        try {
            Integer count = future.join();
            return count == null ? unavailable : String.valueOf(count);
        } catch (Exception exception) {
            return unavailable;
        }
    }

    private String label(String name, boolean value) {
        String state = value ? messages.raw("command.debug-yes") : messages.raw("command.debug-no");
        return name + ": " + state;
    }
}
