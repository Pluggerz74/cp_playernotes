package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.model.PlayerNote;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GuiManager {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private static final Set<Integer> ACCENT_SLOTS = Set.of(
            4,
            9, 11, 13, 15, 17,
            27, 35,
            46, 48, 50, 52
    );

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;
    private final GuiItemBuilder itemBuilder;
    private final Map<UUID, PlayerNotesGui> openMenus = new HashMap<>();

    public GuiManager(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.itemBuilder = new GuiItemBuilder(plugin);
    }

    public void openPlayerNotes(Player viewer, OfflinePlayer target, String targetName) {
        plugin.notes().findByTarget(target.getUniqueId(), false).whenComplete((notes, error) ->
                CommandSupport.runSync(plugin, () -> {
                    if (!viewer.isOnline()) {
                        return;
                    }

                    if (error != null) {
                        plugin.logSevere("Failed to load notes for GUI: " + targetName, error);
                        messages.send(viewer, "command.error");
                        return;
                    }

                    openPlayerNotes(viewer, target.getUniqueId(), targetName, notes, 0);
                }));
    }

    public void openPlayerNotes(Player viewer, UUID targetUuid, String targetName, List<PlayerNote> notes, int page) {
        PlayerNotesGui menu = buildMenu(targetUuid, targetName, notes, page);
        Inventory inventory = Bukkit.createInventory(menu, itemBuilder.inventorySize(), itemBuilder.titleComponent(targetName));
        menu.bindInventory(inventory);
        populateInventory(menu, inventory);
        openMenus.put(viewer.getUniqueId(), menu);
        viewer.openInventory(inventory);
    }

    public PlayerNotesGui getOpenMenu(UUID viewerUuid) {
        return openMenus.get(viewerUuid);
    }

    public void closeMenu(UUID viewerUuid) {
        openMenus.remove(viewerUuid);
    }

    public MessageService messages() {
        return messages;
    }

    private PlayerNotesGui buildMenu(UUID targetUuid, String targetName, List<PlayerNote> notes, int page) {
        return new PlayerNotesGui(targetUuid, targetName, notes, page);
    }

    private void populateInventory(PlayerNotesGui menu, Inventory inventory) {
        Set<Integer> interactiveSlots = new HashSet<>();

        int profileSlot = itemBuilder.slot("player-profile", 10);
        int totalSlot = itemBuilder.slot("total-notes", 12);
        int criticalSlot = itemBuilder.slot("critical-notes", 14);
        int addSlot = itemBuilder.slot("add-note", 16);
        int previousSlot = itemBuilder.slot("previous", 45);
        int closeSlot = itemBuilder.slot("close", 49);
        int nextSlot = itemBuilder.slot("next", 53);

        interactiveSlots.add(profileSlot);
        interactiveSlots.add(totalSlot);
        interactiveSlots.add(criticalSlot);
        interactiveSlots.add(addSlot);
        interactiveSlots.add(previousSlot);
        interactiveSlots.add(closeSlot);
        interactiveSlots.add(nextSlot);

        int noteSlots = itemBuilder.noteSlotCount();
        int noteStart = itemBuilder.slot("note-start", 28);
        for (int index = 0; index < noteSlots; index++) {
            interactiveSlots.add(noteStart + index);
        }

        fillFrame(inventory, interactiveSlots);

        int totalNotes = menu.notes().size();
        int criticalNotes = (int) menu.notes().stream().filter(PlayerNote::isCritical).count();

        inventory.setItem(profileSlot, itemBuilder.playerProfile(menu.targetName(), menu.targetUuid()));
        inventory.setItem(totalSlot, itemBuilder.totalNotesStat(totalNotes));
        inventory.setItem(criticalSlot, itemBuilder.criticalNotesStat(criticalNotes));
        inventory.setItem(addSlot, itemBuilder.addNoteButton());
        inventory.setItem(previousSlot, itemBuilder.previousButton());
        inventory.setItem(closeSlot, itemBuilder.closeButton());
        inventory.setItem(nextSlot, itemBuilder.nextButton());

        menu.registerAction(addSlot, new PlayerNotesGui.SlotAction(PlayerNotesGui.ActionType.ADD_NOTE, null));
        menu.registerAction(previousSlot, new PlayerNotesGui.SlotAction(PlayerNotesGui.ActionType.PREVIOUS, null));
        menu.registerAction(closeSlot, new PlayerNotesGui.SlotAction(PlayerNotesGui.ActionType.CLOSE, null));
        menu.registerAction(nextSlot, new PlayerNotesGui.SlotAction(PlayerNotesGui.ActionType.NEXT, null));

        int startIndex = menu.page() * noteSlots;

        for (int index = 0; index < noteSlots; index++) {
            int slot = noteStart + index;
            int noteIndex = startIndex + index;

            if (noteIndex >= menu.notes().size()) {
                inventory.setItem(slot, itemBuilder.innerPane());
                continue;
            }

            PlayerNote note = menu.notes().get(noteIndex);
            inventory.setItem(slot, itemBuilder.noteItem(
                    note.getId(),
                    note.getType().name(),
                    note.getPriority().name(),
                    note.getContent(),
                    DATE_TIME.format(note.getCreatedAt()),
                    note.getStaffName(),
                    itemBuilder.noteMaterial(note.getType().name(), note.getPriority().name()),
                    note.isCritical()
            ));
            menu.registerAction(slot, new PlayerNotesGui.SlotAction(PlayerNotesGui.ActionType.NOTE, note.getId()));
        }
    }

    private void fillFrame(Inventory inventory, Set<Integer> interactiveSlots) {
        int size = inventory.getSize();

        for (int slot = 0; slot < size; slot++) {
            if (interactiveSlots.contains(slot)) {
                continue;
            }

            if (isBorder(slot, size)) {
                inventory.setItem(slot, itemBuilder.borderPane());
            } else if (ACCENT_SLOTS.contains(slot)) {
                inventory.setItem(slot, itemBuilder.accentPane());
            } else {
                inventory.setItem(slot, itemBuilder.innerPane());
            }
        }
    }

    private boolean isBorder(int slot, int size) {
        int row = slot / 9;
        int column = slot % 9;
        int lastRow = (size / 9) - 1;
        return row == 0 || row == lastRow || column == 0 || column == 8;
    }
}
