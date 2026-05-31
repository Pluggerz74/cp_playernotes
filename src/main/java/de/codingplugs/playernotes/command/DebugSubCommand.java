package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.DiagnosticsService;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class DebugSubCommand implements SubCommand {

    private final DiagnosticsService diagnostics;

    public DebugSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.diagnostics = new DiagnosticsService(plugin, messages);
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public String permission() {
        return Permissions.ADMIN;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        diagnostics.sendDebugReport(sender);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
