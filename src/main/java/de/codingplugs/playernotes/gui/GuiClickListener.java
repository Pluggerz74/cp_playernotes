package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.model.PlayerNote;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.ChatInputService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class GuiClickListener implements Listener {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final GuiManager guiManager;
    private final ChatInputService chatInputService;

    public GuiClickListener(GuiManager guiManager, ChatInputService chatInputService) {
        this.guiManager = guiManager;
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PlayerNotesGui menu)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != menu) {
            return;
        }

        PlayerNotesGui.SlotAction action = menu.actionForSlot(event.getSlot());
        if (action == null) {
            return;
        }

        switch (action.type()) {
            case CLOSE -> player.closeInventory();
            case ADD_NOTE -> handleAddNote(player, menu);
            case PREVIOUS, NEXT -> guiManager.messages().send(player, "gui.pagination-placeholder");
            case NOTE -> handleNoteClick(player, menu, action.noteId());
        }
    }

    private void handleAddNote(Player player, PlayerNotesGui menu) {
        if (!CommandSupport.hasPermission(player, Permissions.ADD)) {
            guiManager.messages().send(player, "command.no-permission");
            return;
        }

        player.closeInventory();
        chatInputService.startInput(player, menu.targetUuid(), menu.targetName());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PlayerNotesGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof PlayerNotesGui) {
            guiManager.closeMenu(player.getUniqueId());
        }
    }

    private void handleNoteClick(Player player, PlayerNotesGui menu, Long noteId) {
        if (noteId == null) {
            return;
        }

        PlayerNote note = menu.noteById(noteId);
        if (note == null) {
            guiManager.messages().send(player, "command.note-not-found", Map.of("id", String.valueOf(noteId)));
            return;
        }

        guiManager.messages().send(player, "gui.note-detail", Map.of(
                "id", String.valueOf(note.getId()),
                "player", menu.targetName(),
                "type", note.getType().name(),
                "priority", note.getPriority().name(),
                "content", note.getContent(),
                "staff", note.getStaffName(),
                "date", DATE_TIME.format(note.getCreatedAt())
        ));
    }
}
