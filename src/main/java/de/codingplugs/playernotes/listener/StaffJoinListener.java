package de.codingplugs.playernotes.listener;

import de.codingplugs.playernotes.service.JoinAlertService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class StaffJoinListener implements Listener {

    private final JoinAlertService joinAlertService;

    public StaffJoinListener(JoinAlertService joinAlertService) {
        this.joinAlertService = joinAlertService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        joinAlertService.handlePlayerJoin(event.getPlayer());
    }
}
