package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.model.NoteFilterMode;
import de.codingplugs.playernotes.model.PlayerNote;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.ChatInputService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public final class GuiClickListener implements Listener {

    private final GuiManager guiManager;
    private final ChatInputService chatInputService;

    public GuiClickListener(GuiManager guiManager, ChatInputService chatInputService) {
        this.guiManager = guiManager;
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof PlayerNotesGui menu) {
            handlePlayerNotesClick(event, menu);
        } else if (holder instanceof NoteDetailGui detail) {
            handleNoteDetailClick(event, detail);
        } else if (holder instanceof NoteTypeSelectGui typeSelect) {
            handleTypeSelectClick(event, typeSelect);
        } else if (holder instanceof NotePrioritySelectGui prioritySelect) {
            handlePrioritySelectClick(event, prioritySelect);
        }
    }

    private void handlePlayerNotesClick(InventoryClickEvent event, PlayerNotesGui menu) {
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
            case PREVIOUS -> guiManager.loadAndOpenPlayerNotes(
                    player,
                    menu.targetUuid(),
                    menu.targetName(),
                    menu.page() - 1,
                    menu.filterMode()
            );
            case NEXT -> guiManager.loadAndOpenPlayerNotes(
                    player,
                    menu.targetUuid(),
                    menu.targetName(),
                    menu.page() + 1,
                    menu.filterMode()
            );
            case FILTER -> guiManager.loadAndOpenPlayerNotes(
                    player,
                    menu.targetUuid(),
                    menu.targetName(),
                    0,
                    menu.filterMode().next()
            );
            case NOTE -> handleNoteClick(player, menu, action.noteId());
        }
    }

    private void handleNoteDetailClick(InventoryClickEvent event, NoteDetailGui detail) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != detail) {
            return;
        }

        NoteDetailGui.SlotAction action = detail.actionForSlot(event.getSlot());
        if (action == null) {
            return;
        }

        switch (action.type()) {
            case CLOSE -> player.closeInventory();
            case BACK -> guiManager.reopenPlayerNotes(
                    player,
                    detail.targetUuid(),
                    detail.targetName(),
                    detail.page(),
                    detail.filterMode()
            );
            case ARCHIVE -> handleArchive(player, detail);
            case EDIT -> handleEdit(player, detail);
            case DELETE -> handleDelete(player, detail);
        }
    }

    private void handleTypeSelectClick(InventoryClickEvent event, NoteTypeSelectGui menu) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != menu) {
            return;
        }

        NoteTypeSelectGui.SlotAction action = menu.actionForSlot(event.getSlot());
        if (action == null) {
            return;
        }

        switch (action.type()) {
            case CLOSE -> player.closeInventory();
            case BACK -> guiManager.reopenPlayerNotes(
                    player,
                    menu.targetUuid(),
                    menu.targetName(),
                    menu.page(),
                    menu.filterMode()
            );
            case SELECT -> {
                if (action.noteType() == null) {
                    return;
                }
                guiManager.openPrioritySelect(
                        player,
                        menu.targetUuid(),
                        menu.targetName(),
                        action.noteType(),
                        menu.page(),
                        menu.filterMode()
                );
            }
        }
    }

    private void handlePrioritySelectClick(InventoryClickEvent event, NotePrioritySelectGui menu) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != menu) {
            return;
        }

        NotePrioritySelectGui.SlotAction action = menu.actionForSlot(event.getSlot());
        if (action == null) {
            return;
        }

        switch (action.type()) {
            case CLOSE -> player.closeInventory();
            case BACK -> guiManager.openTypeSelect(
                    player,
                    menu.targetUuid(),
                    menu.targetName(),
                    menu.page(),
                    menu.filterMode()
            );
            case SELECT -> {
                if (action.priority() == null) {
                    return;
                }
                guiManager.startNoteTextInput(
                        player,
                        menu.targetUuid(),
                        menu.targetName(),
                        menu.noteType(),
                        action.priority(),
                        menu.page(),
                        menu.filterMode()
                );
            }
        }
    }

    private void handleAddNote(Player player, PlayerNotesGui menu) {
        if (!CommandSupport.hasPermission(player, Permissions.ADD)) {
            guiManager.messages().send(player, "command.no-permission");
            return;
        }

        guiManager.openTypeSelect(
                player,
                menu.targetUuid(),
                menu.targetName(),
                menu.page(),
                menu.filterMode()
        );
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

        guiManager.openNoteDetail(
                player,
                note,
                menu.targetUuid(),
                menu.targetName(),
                menu.page(),
                menu.filterMode()
        );
    }

    private void handleArchive(Player player, NoteDetailGui detail) {
        if (!CommandSupport.hasPermission(player, Permissions.ARCHIVE)) {
            guiManager.messages().send(player, "command.no-permission");
            return;
        }

        guiManager.archiveNoteFromGui(player, detail);
    }

    private void handleEdit(Player player, NoteDetailGui detail) {
        if (!CommandSupport.hasPermission(player, Permissions.EDIT)) {
            guiManager.messages().send(player, "command.no-permission");
            return;
        }

        guiManager.startNoteEditInput(player, detail);
    }

    private void handleDelete(Player player, NoteDetailGui detail) {
        if (!CommandSupport.hasPermission(player, Permissions.REMOVE)) {
            guiManager.messages().send(player, "command.no-permission");
            return;
        }

        guiManager.deleteNoteFromGui(player, detail);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof PlayerNotesGui
                || holder instanceof NoteDetailGui
                || holder instanceof NoteTypeSelectGui
                || holder instanceof NotePrioritySelectGui) {
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
}
