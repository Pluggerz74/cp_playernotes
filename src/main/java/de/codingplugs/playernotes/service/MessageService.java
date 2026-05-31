package de.codingplugs.playernotes.service;

import de.codingplugs.playernotes.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Map;

public final class MessageService {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

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
        return configManager.messages().getString(path, "");
    }

    public String format(String path, Map<String, String> placeholders) {
        String message = raw(path);
        if (message.isEmpty()) {
            return "";
        }

        message = message.replace("<prefix>", prefix);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("<" + entry.getKey() + ">", entry.getValue());
        }

        return message;
    }

    public Component component(String path) {
        return component(path, Collections.emptyMap());
    }

    public Component component(String path, Map<String, String> placeholders) {
        return MINI_MESSAGE.deserialize(format(path, placeholders));
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(component(path, placeholders));
    }

    public void sendLines(CommandSender sender, String path, Map<String, String> placeholders) {
        String content = format(path, placeholders);
        if (content.isEmpty()) {
            return;
        }

        for (String line : content.split("\n")) {
            sender.sendMessage(MINI_MESSAGE.deserialize(line));
        }
    }
}
