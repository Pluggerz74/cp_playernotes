package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.command.CommandSupport;
import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.model.NoteType;
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

    private static final Set<Integer> DETAIL_ACCENT_SLOTS = Set.of(
            1, 3, 4, 5, 7,
            9, 11, 13, 15, 17,
            21, 23
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

    public void openNoteDetail(Player viewer, PlayerNote note, UUID targetUuid, String targetName, int page) {
        NoteDetailGui detail = new NoteDetailGui(note, targetUuid, targetName, page);
        Inventory inventory = Bukkit.createInventory(
                detail,
                itemBuilder.detailInventorySize(),
                itemBuilder.detailTitleComponent(note.getId())
        );
        detail.bindInventory(inventory);
        populateDetailInventory(detail, inventory, note);
        viewer.openInventory(inventory);
    }

    public void reopenPlayerNotes(Player viewer, UUID targetUuid, String targetName, int page) {
        plugin.notes().findByTarget(targetUuid, false).whenComplete((notes, error) ->
                CommandSupport.runSync(plugin, () -> {
                    if (!viewer.isOnline()) {
                        return;
                    }

                    if (error != null) {
                        plugin.logSevere("Failed to reload notes for " + targetName, error);
                        messages.send(viewer, "command.error");
                        return;
                    }

                    openPlayerNotes(viewer, targetUuid, targetName, notes, page);
                }));
    }

    public void archiveNoteFromGui(Player viewer, NoteDetailGui detail) {
        long noteId = detail.note().getId();
        plugin.notes().archiveNote(noteId).whenComplete((archived, error) ->
                CommandSupport.deliverFeedback(plugin, viewer, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to archive note #" + noteId, error);
                        messages.send(viewer, "command.database-error");
                        return;
                    }

                    if (archived == null || !archived) {
                        messages.send(viewer, "command.note-not-found", Map.of("id", String.valueOf(noteId)));
                        return;
                    }

                    messages.send(viewer, "command.archive-success", Map.of("id", String.valueOf(noteId)));
                    reopenPlayerNotes(viewer, detail.targetUuid(), detail.targetName(), detail.page());
                }));
    }

    public void deleteNoteFromGui(Player viewer, NoteDetailGui detail) {
        long noteId = detail.note().getId();
        plugin.notes().deleteNote(noteId).whenComplete((removed, error) ->
                CommandSupport.deliverFeedback(plugin, viewer, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Failed to delete note #" + noteId, error);
                        messages.send(viewer, "command.database-error");
                        return;
                    }

                    if (removed == null || !removed) {
                        messages.send(viewer, "command.note-not-found", Map.of("id", String.valueOf(noteId)));
                        return;
                    }

                    messages.send(viewer, "command.remove-success", Map.of("id", String.valueOf(noteId)));
                    reopenPlayerNotes(viewer, detail.targetUuid(), detail.targetName(), detail.page());
                }));
    }

    public void openTypeSelect(Player staff, UUID targetUuid, String targetName, int page) {
        NoteTypeSelectGui menu = new NoteTypeSelectGui(targetUuid, targetName, page);
        Inventory inventory = Bukkit.createInventory(
                menu,
                itemBuilder.selectionInventorySize("type-select-menu"),
                itemBuilder.typeSelectTitleComponent(targetName)
        );
        menu.bindInventory(inventory);
        populateTypeSelectInventory(menu, inventory);
        staff.openInventory(inventory);
    }

    public void openPrioritySelect(Player staff, UUID targetUuid, String targetName, NoteType type, int page) {
        NotePrioritySelectGui menu = new NotePrioritySelectGui(targetUuid, targetName, type, page);
        Inventory inventory = Bukkit.createInventory(
                menu,
                itemBuilder.selectionInventorySize("priority-select-menu"),
                itemBuilder.prioritySelectTitleComponent(targetName)
        );
        menu.bindInventory(inventory);
        populatePrioritySelectInventory(menu, inventory);
        staff.openInventory(inventory);
    }

    public void startNoteTextInput(
            Player staff,
            UUID targetUuid,
            String targetName,
            NoteType type,
            NotePriority priority,
            int page
    ) {
        staff.closeInventory();
        plugin.chatInput().startInput(staff, targetUuid, targetName, type, priority, page);
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

    private void populateDetailInventory(NoteDetailGui detail, Inventory inventory, PlayerNote note) {
        Set<Integer> interactiveSlots = new HashSet<>();

        int infoSlot = itemBuilder.detailSlot("info", 10);
        int archiveSlot = itemBuilder.detailSlot("archive", 12);
        int deleteSlot = itemBuilder.detailSlot("delete", 14);
        int backSlot = itemBuilder.detailSlot("back", 16);
        int closeSlot = itemBuilder.detailSlot("close", 22);

        interactiveSlots.add(infoSlot);
        interactiveSlots.add(archiveSlot);
        interactiveSlots.add(deleteSlot);
        interactiveSlots.add(backSlot);
        interactiveSlots.add(closeSlot);

        fillDetailFrame(inventory, interactiveSlots);

        String formattedDate = DATE_TIME.format(note.getCreatedAt());
        inventory.setItem(infoSlot, itemBuilder.detailInfoItem(
                note.getId(),
                note.getType().name(),
                note.getPriority().name(),
                note.getContent(),
                formattedDate,
                note.getStaffName()
        ));
        inventory.setItem(archiveSlot, itemBuilder.detailArchiveButton());
        inventory.setItem(deleteSlot, itemBuilder.detailDeleteButton());
        inventory.setItem(backSlot, itemBuilder.detailBackButton());
        inventory.setItem(closeSlot, itemBuilder.detailCloseButton());

        detail.registerAction(archiveSlot, new NoteDetailGui.SlotAction(NoteDetailGui.ActionType.ARCHIVE));
        detail.registerAction(deleteSlot, new NoteDetailGui.SlotAction(NoteDetailGui.ActionType.DELETE));
        detail.registerAction(backSlot, new NoteDetailGui.SlotAction(NoteDetailGui.ActionType.BACK));
        detail.registerAction(closeSlot, new NoteDetailGui.SlotAction(NoteDetailGui.ActionType.CLOSE));
    }

    private void fillDetailFrame(Inventory inventory, Set<Integer> interactiveSlots) {
        fillSmallFrame(inventory, interactiveSlots, DETAIL_ACCENT_SLOTS);
    }

    private void populateTypeSelectInventory(NoteTypeSelectGui menu, Inventory inventory) {
        Set<Integer> interactiveSlots = new HashSet<>();

        int infoSlot = itemBuilder.selectionSlot("type-select-menu", "info", 10);
        int warningSlot = itemBuilder.selectionSlot("type-select-menu", "warning", 11);
        int suspectSlot = itemBuilder.selectionSlot("type-select-menu", "suspect", 12);
        int punishmentSlot = itemBuilder.selectionSlot("type-select-menu", "punishment", 13);
        int staffSlot = itemBuilder.selectionSlot("type-select-menu", "staff", 14);
        int backSlot = itemBuilder.selectionSlot("type-select-menu", "back", 16);
        int closeSlot = itemBuilder.selectionSlot("type-select-menu", "close", 22);

        interactiveSlots.addAll(Set.of(infoSlot, warningSlot, suspectSlot, punishmentSlot, staffSlot, backSlot, closeSlot));
        fillSmallFrame(inventory, interactiveSlots, DETAIL_ACCENT_SLOTS);

        inventory.setItem(infoSlot, itemBuilder.typeOptionItem(NoteType.INFO));
        inventory.setItem(warningSlot, itemBuilder.typeOptionItem(NoteType.WARNING));
        inventory.setItem(suspectSlot, itemBuilder.typeOptionItem(NoteType.SUSPECT));
        inventory.setItem(punishmentSlot, itemBuilder.typeOptionItem(NoteType.PUNISHMENT));
        inventory.setItem(staffSlot, itemBuilder.typeOptionItem(NoteType.STAFF));
        inventory.setItem(backSlot, itemBuilder.selectionBackButton("type-select-menu"));
        inventory.setItem(closeSlot, itemBuilder.selectionCloseButton("type-select-menu"));

        menu.registerAction(infoSlot, new NoteTypeSelectGui.SlotAction(NoteTypeSelectGui.ActionType.SELECT, NoteType.INFO));
        menu.registerAction(warningSlot, new NoteTypeSelectGui.SlotAction(NoteTypeSelectGui.ActionType.SELECT, NoteType.WARNING));
        menu.registerAction(suspectSlot, new NoteTypeSelectGui.SlotAction(NoteTypeSelectGui.ActionType.SELECT, NoteType.SUSPECT));
        menu.registerAction(punishmentSlot, new NoteTypeSelectGui.SlotAction(NoteTypeSelectGui.ActionType.SELECT, NoteType.PUNISHMENT));
        menu.registerAction(staffSlot, new NoteTypeSelectGui.SlotAction(NoteTypeSelectGui.ActionType.SELECT, NoteType.STAFF));
        menu.registerAction(backSlot, new NoteTypeSelectGui.SlotAction(NoteTypeSelectGui.ActionType.BACK, null));
        menu.registerAction(closeSlot, new NoteTypeSelectGui.SlotAction(NoteTypeSelectGui.ActionType.CLOSE, null));
    }

    private void populatePrioritySelectInventory(NotePrioritySelectGui menu, Inventory inventory) {
        Set<Integer> interactiveSlots = new HashSet<>();

        int lowSlot = itemBuilder.selectionSlot("priority-select-menu", "low", 10);
        int normalSlot = itemBuilder.selectionSlot("priority-select-menu", "normal", 11);
        int highSlot = itemBuilder.selectionSlot("priority-select-menu", "high", 12);
        int criticalSlot = itemBuilder.selectionSlot("priority-select-menu", "critical", 13);
        int backSlot = itemBuilder.selectionSlot("priority-select-menu", "back", 16);
        int closeSlot = itemBuilder.selectionSlot("priority-select-menu", "close", 22);

        interactiveSlots.addAll(Set.of(lowSlot, normalSlot, highSlot, criticalSlot, backSlot, closeSlot));
        fillSmallFrame(inventory, interactiveSlots, DETAIL_ACCENT_SLOTS);

        inventory.setItem(lowSlot, itemBuilder.priorityOptionItem(NotePriority.LOW));
        inventory.setItem(normalSlot, itemBuilder.priorityOptionItem(NotePriority.NORMAL));
        inventory.setItem(highSlot, itemBuilder.priorityOptionItem(NotePriority.HIGH));
        inventory.setItem(criticalSlot, itemBuilder.priorityOptionItem(NotePriority.CRITICAL));
        inventory.setItem(backSlot, itemBuilder.selectionBackButton("priority-select-menu"));
        inventory.setItem(closeSlot, itemBuilder.selectionCloseButton("priority-select-menu"));

        menu.registerAction(lowSlot, new NotePrioritySelectGui.SlotAction(NotePrioritySelectGui.ActionType.SELECT, NotePriority.LOW));
        menu.registerAction(normalSlot, new NotePrioritySelectGui.SlotAction(NotePrioritySelectGui.ActionType.SELECT, NotePriority.NORMAL));
        menu.registerAction(highSlot, new NotePrioritySelectGui.SlotAction(NotePrioritySelectGui.ActionType.SELECT, NotePriority.HIGH));
        menu.registerAction(criticalSlot, new NotePrioritySelectGui.SlotAction(NotePrioritySelectGui.ActionType.SELECT, NotePriority.CRITICAL));
        menu.registerAction(backSlot, new NotePrioritySelectGui.SlotAction(NotePrioritySelectGui.ActionType.BACK, null));
        menu.registerAction(closeSlot, new NotePrioritySelectGui.SlotAction(NotePrioritySelectGui.ActionType.CLOSE, null));
    }

    private void fillSmallFrame(Inventory inventory, Set<Integer> interactiveSlots, Set<Integer> accentSlots) {
        int size = inventory.getSize();

        for (int slot = 0; slot < size; slot++) {
            if (interactiveSlots.contains(slot)) {
                continue;
            }

            if (isBorder(slot, size)) {
                inventory.setItem(slot, itemBuilder.borderPane());
            } else if (accentSlots.contains(slot)) {
                inventory.setItem(slot, itemBuilder.accentPane());
            } else {
                inventory.setItem(slot, itemBuilder.innerPane());
            }
        }
    }
}
