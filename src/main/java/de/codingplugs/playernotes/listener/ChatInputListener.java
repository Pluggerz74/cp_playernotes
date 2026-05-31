package de.codingplugs.playernotes.listener;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.service.ChatInputService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatInputListener implements Listener {

    private final PlayerNotesPlugin plugin;
    private final ChatInputService chatInputService;

    public ChatInputListener(PlayerNotesPlugin plugin, ChatInputService chatInputService) {
        this.plugin = plugin;
        this.chatInputService = chatInputService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!chatInputService.hasPendingInput(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        CommandSupport.runSync(plugin, () -> chatInputService.handleMessage(player, message));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        chatInputService.cancelInput(event.getPlayer().getUniqueId());
    }
}
