package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class ReloadSubCommand implements SubCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public ReloadSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return Permissions.RELOAD;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        try {
            plugin.reloadPlugin();
            messages.send(sender, "command.reload-success");
        } catch (Exception exception) {
            plugin.logSevere("Reload failed", exception);
            messages.send(sender, "command.reload-failure");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
