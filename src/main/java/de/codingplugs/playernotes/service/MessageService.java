package de.codingplugs.playernotes.service;

import de.codingplugs.playernotes.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Map;

public final class MessageService {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Map<String, String> FALLBACKS = Map.of(
            "command.add-success", "<green>✔ Note added for <white><player></white>.</green> <gray>ID: #<id></gray>",
            "command.archive-success", "<green>✔ Note #<id> archived.</green>",
            "command.remove-success", "<red>✔ Note #<id> permanently removed.</red>",
            "command.note-not-found", "<red>No note found with id #<id>.</red>",
            "command.database-error", "<red>Something went wrong while updating notes. Check console.</red>"
    );

    private final ConfigManager configManager;

    private String prefix = "";

    public MessageService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void load() {
        reload();
    }

    public void reload() {
        FileConfiguration messages = configManager.messages();
        prefix = messages.getString("prefix", "<gray>[<aqua>PlayerNotes</aqua>]</gray> ");
    }

    public String raw(String path) {
        String message = configManager.messages().getString(path, "");
        if (message == null || message.isBlank()) {
            return FALLBACKS.getOrDefault(path, "");
        }
        return message;
    }

    public String format(String path, Map<String, String> placeholders) {
        return MiniMessage.miniMessage().serialize(component(path, placeholders));
    }

    public Component component(String path) {
        return component(path, Collections.emptyMap());
    }

    public Component component(String path, Map<String, String> placeholders) {
        String template = raw(path);
        if (template.isBlank()) {
            return Component.empty();
        }

        template = template.replace("<prefix>", prefix);
        return MINI_MESSAGE.deserialize(template, tagResolver(placeholders));
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        try {
            Component message = component(path, placeholders);
            if (message.equals(Component.empty())) {
                sender.sendMessage(fallbackComponent(path, placeholders));
                return;
            }
            sender.sendMessage(message);
        } catch (Exception exception) {
            sender.sendMessage(fallbackComponent(path, placeholders));
        }
    }

    public void sendLines(CommandSender sender, String path, Map<String, String> placeholders) {
        String content = raw(path);
        if (content.isBlank()) {
            return;
        }

        content = content.replace("<prefix>", prefix);

        for (String line : content.split("\n")) {
            sender.sendMessage(MINI_MESSAGE.deserialize(line, tagResolver(placeholders)));
        }
    }

    private Component fallbackComponent(String path, Map<String, String> placeholders) {
        String template = FALLBACKS.get(path);
        if (template == null || template.isBlank()) {
            return MINI_MESSAGE.deserialize("<red>Something went wrong while updating notes. Check console.</red>");
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
}
