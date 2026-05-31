package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class VersionSubCommand implements SubCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public VersionSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String permission() {
        return Permissions.USE;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String authors = plugin.getDescription().getAuthors().stream()
                .collect(Collectors.joining(", "));

        messages.sendLines(sender, "command.version", Map.of(
                "plugin", plugin.getDescription().getName(),
                "version", plugin.getDescription().getVersion(),
                "author", authors.isEmpty() ? "Unknown" : authors,
                "supported", PlayerNotesPlugin.SUPPORTED_SERVER_VERSIONS,
                "java", System.getProperty("java.version")
        ));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
