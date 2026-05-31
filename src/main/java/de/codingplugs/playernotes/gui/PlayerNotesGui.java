package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.model.NoteFilterMode;
import de.codingplugs.playernotes.model.PlayerNote;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerNotesGui implements InventoryHolder {

    public enum ActionType {
        CLOSE,
        ADD_NOTE,
        PREVIOUS,
        NEXT,
        FILTER,
        NOTE
    }

    public record SlotAction(ActionType type, Long noteId) {
    }

    private Inventory inventory;
    private final UUID targetUuid;
    private final String targetName;
    private final List<PlayerNote> notes;
    private final int page;
    private final NoteFilterMode filterMode;
    private final Map<Integer, SlotAction> slotActions = new HashMap<>();

    public PlayerNotesGui(
            UUID targetUuid,
            String targetName,
            List<PlayerNote> notes,
            int page,
            NoteFilterMode filterMode
    ) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.notes = List.copyOf(notes);
        this.page = page;
        this.filterMode = filterMode;
    }

    public void bindInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID targetUuid() {
        return targetUuid;
    }

    public String targetName() {
        return targetName;
    }

    public List<PlayerNote> notes() {
        return notes;
    }

    public int page() {
        return page;
    }

    public NoteFilterMode filterMode() {
        return filterMode;
    }

    public void registerAction(int slot, SlotAction action) {
        slotActions.put(slot, action);
    }

    public SlotAction actionForSlot(int slot) {
        return slotActions.get(slot);
    }

    public PlayerNote noteById(long noteId) {
        return notes.stream()
                .filter(note -> note.getId() == noteId)
                .findFirst()
                .orElse(null);
    }
}
