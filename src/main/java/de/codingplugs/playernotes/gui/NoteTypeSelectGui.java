package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.model.NoteType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NoteTypeSelectGui implements InventoryHolder {

    public enum ActionType {
        SELECT,
        BACK,
        CLOSE
    }

    public record SlotAction(ActionType type, NoteType noteType) {
    }

    private Inventory inventory;
    private final UUID targetUuid;
    private final String targetName;
    private final int page;
    private final Map<Integer, SlotAction> slotActions = new HashMap<>();

    public NoteTypeSelectGui(UUID targetUuid, String targetName, int page) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.page = page;
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

    public int page() {
        return page;
    }

    public void registerAction(int slot, SlotAction action) {
        slotActions.put(slot, action);
    }

    public SlotAction actionForSlot(int slot) {
        return slotActions.get(slot);
    }
}
