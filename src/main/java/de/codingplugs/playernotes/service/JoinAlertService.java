package de.codingplugs.playernotes.service;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.permission.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class JoinAlertService {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public JoinAlertService(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
    }

    public void handlePlayerJoin(Player joined) {
        if (!isEnabled()) {
            return;
        }

        UUID targetUuid = joined.getUniqueId();
        String targetName = joined.getName() != null ? joined.getName() : targetUuid.toString();
        NotePriority minimumPriority = minimumPriority();

        plugin.notes().countActiveNotesAtOrAbovePriority(targetUuid, minimumPriority).whenComplete((count, error) ->
                CommandSupport.runSync(plugin, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to check join alerts for " + targetName, error);
                        return;
                    }

                    if (count == null || count <= 0) {
                        return;
                    }

                    notifyStaff(joined, targetName, count);
                }));
    }

    private void notifyStaff(Player joined, String targetName, int count) {
        String permission = notifyPermission();

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!shouldNotify(staff, joined, permission)) {
                continue;
            }

            messages.send(staff, "join-alerts.staff-alert", Map.of(
                    "player", targetName,
                    "count", String.valueOf(count)
            ));
            messages.send(staff, "join-alerts.staff-action", Map.of("player", targetName));
        }
    }

    private boolean shouldNotify(Player staff, Player joined, String permission) {
        if (!staff.hasPermission(Permissions.ADMIN) && !staff.hasPermission(permission)) {
            return false;
        }

        if (staff.getUniqueId().equals(joined.getUniqueId())) {
            return staff.hasPermission(Permissions.ADMIN) || staff.hasPermission(permission);
        }

        return true;
    }

    private boolean isEnabled() {
        return config().getBoolean("join-alerts.enabled", true);
    }

    private NotePriority minimumPriority() {
        return NotePriority.fromConfig(config().getString("join-alerts.minimum-priority", "HIGH"));
    }

    private String notifyPermission() {
        return config().getString("join-alerts.permission", Permissions.NOTIFY);
    }

    private FileConfiguration config() {
        return plugin.configManager().config();
    }
}
