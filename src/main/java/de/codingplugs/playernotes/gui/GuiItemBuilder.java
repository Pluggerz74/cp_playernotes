package de.codingplugs.playernotes.gui;

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

    public ItemStack closeButton() {
        return labeledItem(
                getMaterial("materials.close", Material.BARRIER),
                "labels.close-name",
                "labels.close-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack previousButton() {
        return labeledItem(
                getMaterial("materials.previous", Material.ARROW),
                "labels.previous-name",
                "labels.previous-lore",
                Collections.emptyMap()
        );
    }

    public ItemStack nextButton() {
        return labeledItem(
                getMaterial("materials.next", Material.ARROW),
                "labels.next-name",
                "labels.next-lore",
                Collections.emptyMap()
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
