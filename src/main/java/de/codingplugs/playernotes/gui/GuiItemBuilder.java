package de.codingplugs.playernotes.gui;

import de.codingplugs.playernotes.model.NoteFilterMode;
import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.model.NoteType;
import de.codingplugs.playernotes.PlayerNotesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GuiItemBuilder {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Component FILLER_NAME = Component.text(" ");

    private final PlayerNotesPlugin plugin;

    public GuiItemBuilder(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack borderPane() {
        return fillerPane(getMaterial("materials.border", Material.WHITE_STAINED_GLASS_PANE));
    }

    public ItemStack innerPane() {
        return fillerPane(getMaterial("materials.background", Material.LIGHT_GRAY_STAINED_GLASS_PANE));
    }

    public ItemStack accentPane() {
        return fillerPane(getMaterial("materials.accent", Material.LIGHT_BLUE_STAINED_GLASS_PANE));
    }

    public ItemStack playerProfile(String playerName, UUID playerUuid) {
        ItemStack item = item(getMaterial("materials.player-profile", Material.PLAYER_HEAD));
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(playerUuid));
            meta.displayName(text("labels.player-profile-name", Map.of("player", playerName)));
            meta.lore(lore("labels.player-profile-lore", Map.of("player", playerName)));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack totalNotesStat(int count) {
        return labeledItem(
                getMaterial("materials.stat-total", Material.PAPER),
                "labels.total-name",
                "labels.total-lore",
                Map.of("count", String.valueOf(count))
        );
    }

    public ItemStack criticalNotesStat(int count) {
        return labeledItem(
                getMaterial("materials.stat-critical", Material.RED_DYE),
                "labels.critical-name",
                "labels.critical-lore",
                Map.of("count", String.valueOf(count))
        );
    }

    public ItemStack addNoteButton() {
        return labeledItem(
                getMaterial("materials.add-note", Material.WRITABLE_BOOK),
                "labels.add-name",
                "labels.add-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack noteItem(
            long id,
            String type,
            String priority,
            String content,
            String date,
            String staff,
            Material material,
            boolean critical
    ) {
        Map<String, String> placeholders = Map.of(
                "id", String.valueOf(id),
                "type", type,
                "priority", priority,
                "content", truncate(content, 64),
                "date", date,
                "staff", staff
        );

        String namePath = critical ? "labels.note-critical-name" : "labels.note-name";
        return labeledItem(material, namePath, "labels.note-lore", placeholders);
    }

    public ItemStack closeButton(int currentPage, int totalPages) {
        return labeledItem(
                getMaterial("materials.close", Material.BARRIER),
                "labels.close-name",
                "labels.close-lore",
                paginationPlaceholders(currentPage, totalPages)
        );
    }

    public ItemStack filterButton(NoteFilterMode filterMode) {
        return labeledItem(
                getMaterial("materials.filter", Material.HOPPER),
                "labels.filter-name",
                "labels.filter-lore",
                Map.of("filter", filterMode.name())
        );
    }

    public ItemStack previousButtonEnabled(int currentPage, int totalPages) {
        return labeledItem(
                getMaterial("materials.previous", Material.ARROW),
                "labels.previous-enabled-name",
                "labels.previous-enabled-lore",
                paginationPlaceholders(currentPage, totalPages)
        );
    }

    public ItemStack previousButtonDisabled() {
        return labeledItem(
                getMaterial("materials.nav-disabled", Material.GRAY_DYE),
                "labels.previous-disabled-name",
                "labels.previous-disabled-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack nextButtonEnabled(int currentPage, int totalPages) {
        return labeledItem(
                getMaterial("materials.next", Material.ARROW),
                "labels.next-enabled-name",
                "labels.next-enabled-lore",
                paginationPlaceholders(currentPage, totalPages)
        );
    }

    public ItemStack nextButtonDisabled() {
        return labeledItem(
                getMaterial("materials.nav-disabled", Material.GRAY_DYE),
                "labels.next-disabled-name",
                "labels.next-disabled-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack closeButton() {
        return closeButton(0, 1);
    }

    public ItemStack previousButton() {
        return previousButtonDisabled();
    }

    public ItemStack nextButton() {
        return nextButtonDisabled();
    }

    private static Map<String, String> paginationPlaceholders(int currentPage, int totalPages) {
        return Map.of(
                "current", String.valueOf(currentPage + 1),
                "total", String.valueOf(totalPages)
        );
    }

    public Material noteMaterial(String typeName, String priorityName) {
        if ("CRITICAL".equalsIgnoreCase(priorityName)) {
            return getMaterial("materials.note-critical", Material.RED_DYE);
        }
        if ("WARNING".equalsIgnoreCase(typeName) || "SUSPECT".equalsIgnoreCase(typeName)) {
            return getMaterial("materials.note-warning", Material.ORANGE_DYE);
        }
        return getMaterial("materials.note-default", Material.PAPER);
    }

    public int slot(String path, int fallback) {
        return gui().getInt("notes-menu.slots." + path, fallback);
    }

    public int noteSlotCount() {
        int start = slot("note-start", 28);
        int end = slot("note-end", 34);
        return Math.max(0, end - start + 1);
    }

    public Component titleComponent(String playerName) {
        String template = gui().getString(
                "notes-menu.title",
                "<gradient:#ffffff:#60a5fa><bold>Player Notes</bold></gradient> <dark_gray>·</dark_gray> <gray><player></gray>"
        );
        return MINI_MESSAGE.deserialize(template, TagResolver.resolver(Placeholder.unparsed("player", playerName)));
    }

    public int inventorySize() {
        int size = gui().getInt("notes-menu.size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            return 54;
        }
        return size;
    }

    public Component detailTitleComponent(long noteId) {
        String template = gui().getString(
                "detail-menu.title",
                "<gradient:#ffffff:#60a5fa><bold>Note Details</bold></gradient> <dark_gray>·</dark_gray> <gray>#<id></gray>"
        );
        return MINI_MESSAGE.deserialize(template, TagResolver.resolver(Placeholder.unparsed("id", String.valueOf(noteId))));
    }

    public int detailInventorySize() {
        int size = gui().getInt("detail-menu.size", 27);
        if (size % 9 != 0 || size < 9 || size > 54) {
            return 27;
        }
        return size;
    }

    public int detailSlot(String path, int fallback) {
        return gui().getInt("detail-menu.slots." + path, fallback);
    }

    public ItemStack detailInfoItem(long id, String type, String priority, String content, String date, String staff) {
        return labeledItemFromSection(
                "detail-menu",
                detailPriorityMaterial(priority),
                "labels.info-name",
                "labels.info-lore",
                Map.of(
                        "id", String.valueOf(id),
                        "type", type,
                        "priority", priority,
                        "content", content,
                        "date", date,
                        "staff", staff
                )
        );
    }

    public ItemStack detailArchiveButton() {
        return labeledItemFromSection(
                "detail-menu",
                Material.CHEST,
                "labels.archive-name",
                "labels.archive-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack detailEditButton() {
        return labeledItemFromSection(
                "detail-menu",
                Material.WRITABLE_BOOK,
                "labels.edit-name",
                "labels.edit-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack detailDeleteButton() {
        return labeledItemFromSection(
                "detail-menu",
                Material.RED_DYE,
                "labels.delete-name",
                "labels.delete-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack detailBackButton() {
        return labeledItemFromSection(
                "detail-menu",
                Material.ARROW,
                "labels.back-name",
                "labels.back-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack detailCloseButton() {
        return labeledItemFromSection(
                "detail-menu",
                getMaterial("materials.close", Material.BARRIER),
                "labels.close-name",
                "labels.close-lore",
                Collections.emptyMap()
        );
    }

    public Material detailPriorityMaterial(String priorityName) {
        if ("CRITICAL".equalsIgnoreCase(priorityName)) {
            return Material.RED_DYE;
        }
        if ("HIGH".equalsIgnoreCase(priorityName)) {
            return Material.ORANGE_DYE;
        }
        return Material.PAPER;
    }

    public Component typeSelectTitleComponent(String playerName) {
        return selectionTitle("type-select-menu.title", playerName);
    }

    public Component prioritySelectTitleComponent(String playerName) {
        return selectionTitle("priority-select-menu.title", playerName);
    }

    public int selectionInventorySize(String section) {
        int size = gui().getInt(section + ".size", 27);
        if (size % 9 != 0 || size < 9 || size > 54) {
            return 27;
        }
        return size;
    }

    public int selectionSlot(String section, String path, int fallback) {
        return gui().getInt(section + ".slots." + path, fallback);
    }

    public ItemStack typeOptionItem(NoteType type) {
        return labeledItemFromSection(
                "type-select-menu",
                typeSelectMaterial(type),
                "labels.option-name",
                "labels.option-lore",
                Map.of("type", type.name())
        );
    }

    public ItemStack priorityOptionItem(NotePriority priority) {
        return labeledItemFromSection(
                "priority-select-menu",
                prioritySelectMaterial(priority),
                "labels.option-name",
                "labels.option-lore",
                Map.of("priority", priority.name())
        );
    }

    public ItemStack selectionBackButton(String section) {
        return labeledItemFromSection(
                section,
                Material.ARROW,
                "labels.back-name",
                "labels.back-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack selectionCloseButton(String section) {
        return labeledItemFromSection(
                section,
                getMaterial("materials.close", Material.BARRIER),
                "labels.close-name",
                "labels.close-lore",
                Collections.emptyMap()
        );
    }

    public Material typeSelectMaterial(NoteType type) {
        return switch (type) {
            case INFO -> Material.PAPER;
            case WARNING -> Material.ORANGE_DYE;
            case SUSPECT -> firstAvailable(Material.SPYGLASS, Material.COMPASS);
            case PUNISHMENT -> Material.IRON_AXE;
            case STAFF -> Material.NAME_TAG;
        };
    }

    public Material prioritySelectMaterial(NotePriority priority) {
        return switch (priority) {
            case LOW -> Material.LIME_DYE;
            case NORMAL -> Material.LIGHT_BLUE_DYE;
            case HIGH -> Material.ORANGE_DYE;
            case CRITICAL -> Material.RED_DYE;
        };
    }

    private Component selectionTitle(String path, String playerName) {
        String template = gui().getString(
                path,
                "<gradient:#ffffff:#60a5fa><bold>Select</bold></gradient> <dark_gray>·</dark_gray> <gray><player></gray>"
        );
        return MINI_MESSAGE.deserialize(template, TagResolver.resolver(Placeholder.unparsed("player", playerName)));
    }

    private Material firstAvailable(Material primary, Material fallback) {
        return primary.isItem() ? primary : fallback;
    }

    private ItemStack labeledItemFromSection(
            String section,
            Material material,
            String namePath,
            String lorePath,
            Map<String, String> placeholders
    ) {
        ItemStack item = item(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(textFromSection(section, namePath, placeholders));
            meta.lore(loreFromSection(section, lorePath, placeholders));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component textFromSection(String section, String path, Map<String, String> placeholders) {
        String template = gui().getString(section + "." + path, "");
        if (template.isBlank()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(template, tagResolver(placeholders));
    }

    private List<Component> loreFromSection(String section, String path, Map<String, String> placeholders) {
        List<String> lines = gui().getStringList(section + "." + path);
        if (lines.isEmpty()) {
            return List.of();
        }

        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                components.add(Component.empty());
                continue;
            }
            components.add(MINI_MESSAGE.deserialize(line, tagResolver(placeholders)));
        }
        return components;
    }

    private ItemStack labeledItem(Material material, String namePath, String lorePath, Map<String, String> placeholders) {
        ItemStack item = item(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text(namePath, placeholders));
            meta.lore(lore(lorePath, placeholders));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack fillerPane(Material material) {
        ItemStack item = item(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(FILLER_NAME);
            meta.lore(List.of());
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<Component> lore(String path, Map<String, String> placeholders) {
        List<String> lines = gui().getStringList("notes-menu." + path);
        if (lines.isEmpty()) {
            return List.of();
        }

        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                components.add(Component.empty());
                continue;
            }
            components.add(MINI_MESSAGE.deserialize(line, tagResolver(placeholders)));
        }
        return components;
    }

    private Component text(String path, Map<String, String> placeholders) {
        String template = gui().getString("notes-menu." + path, "");
        if (template.isBlank()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(template, tagResolver(placeholders));
    }

    private TagResolver tagResolver(Map<String, String> placeholders) {
        TagResolver.Builder builder = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            builder.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    private Material getMaterial(String path, Material fallback) {
        String name = gui().getString("notes-menu." + path, fallback.name());
        Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }

    private ItemStack item(Material material) {
        return new ItemStack(material);
    }

    private FileConfiguration gui() {
        return plugin.configManager().gui();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
