package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.hook.WebhookResult;
import de.codingplugs.playernotes.permission.Permissions;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class DiscordTestSubCommand implements SubCommand {

    private final PlayerNotesPlugin plugin;
    private final MessageService messages;

    public DiscordTestSubCommand(PlayerNotesPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "discordtest";
    }

    @Override
    public String permission() {
        return Permissions.ADMIN;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        messages.send(sender, "command.discordtest-sending");

        plugin.discord().sendTest(sender.getName()).whenComplete((result, error) ->
                CommandSupport.deliverFeedback(plugin, sender, messages, () -> {
                    if (error != null) {
                        plugin.logSevere("Discord webhook test failed", error);
                        messages.send(sender, "command.discordtest-error");
                        return;
                    }

                    if (result == null) {
                        messages.send(sender, "command.discordtest-error");
                        return;
                    }

                    if (result.success()) {
                        messages.send(sender, "command.discordtest-success");
                        return;
                    }

                    if (result.statusCode() >= 200 && result.statusCode() < 300) {
                        messages.send(sender, "command.discordtest-success");
                        return;
                    }

                    if (result.statusCode() > 0) {
                        messages.send(sender, "command.discordtest-http-failure", Map.of(
                                "status", String.valueOf(result.statusCode())
                        ));
                        return;
                    }

                    messages.send(sender, "command.discordtest-error");
                }));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
