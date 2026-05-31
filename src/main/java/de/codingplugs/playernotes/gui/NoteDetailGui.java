package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.model.NoteFilterMode;
import de.codingplugs.playernotes.model.PlayerNote;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NoteDetailGui implements InventoryHolder {

    public enum ActionType {
        ARCHIVE,
        DELETE,
        BACK,
        CLOSE
    }

    public record SlotAction(ActionType type) {
    }

    private Inventory inventory;
    private final PlayerNote note;
    private final UUID targetUuid;
    private final String targetName;
    private final int page;
    private final NoteFilterMode filterMode;
    private final Map<Integer, SlotAction> slotActions = new HashMap<>();

    public NoteDetailGui(
            PlayerNote note,
            UUID targetUuid,
            String targetName,
            int page,
            NoteFilterMode filterMode
    ) {
        this.note = note;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
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

    public PlayerNote note() {
        return note;
    }

    public UUID targetUuid() {
        return targetUuid;
    }

    public String targetName() {
        return targetName;
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
}
