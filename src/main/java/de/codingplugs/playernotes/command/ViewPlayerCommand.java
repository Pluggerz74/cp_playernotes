package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.gui.GuiManager;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class ViewPlayerCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;
    private final GuiManager guiManager;

    public ViewPlayerCommand(PlayerNotesPlugin plugin, MessageService messages, GuiManager guiManager) {
        this.plugin = plugin;
        this.messages = messages;
        this.guiManager = guiManager;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!CommandSupport.hasPermission(sender, Permissions.VIEW)) {
            messages.send(sender, "command.no-permission");
            return true;
        }

        if (args.length < 1 || args[0].isBlank()) {
            messages.send(sender, "command.view-usage");
            return true;
        }

        if (!(sender instanceof Player viewer)) {
            messages.send(sender, "gui.console-no-gui");
            return true;
        }

        OfflinePlayer target = CommandSupport.resolvePlayer(args[0]).orElse(null);
        if (target == null) {
            messages.send(sender, "command.player-not-found", Map.of("player", args[0]));
            return true;
        }

        String targetName = CommandSupport.displayName(target, args[0]);
        guiManager.openPlayerNotes(viewer, target, targetName);
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!CommandSupport.hasPermission(sender, Permissions.VIEW)) {
            return List.of();
        }

        if (args.length == 1) {
            return CommandSupport.tabCompleteOnlinePlayers(args[0]);
        }

        return List.of();
    }
}
