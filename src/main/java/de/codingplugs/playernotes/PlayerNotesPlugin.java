package de.codingplugs.playernotes;

import de.codingplugs.playernotes.command.PlayerNotesCommand;
import de.codingplugs.playernotes.command.ReloadSubCommand;
import de.codingplugs.playernotes.command.SubCommand;
import de.codingplugs.playernotes.command.VersionSubCommand;
import de.codingplugs.playernotes.config.ConfigManager;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class PlayerNotesPlugin extends JavaPlugin {

    public static final String SUPPORTED_SERVER_VERSIONS = "1.19.4 - 1.21.x";

    private ConfigManager configManager;
    private MessageService messageService;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        if (!configManager.load()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        messageService = new MessageService(configManager);
        messageService.load();

        registerCommands();

        getLogger().info("PlayerNotes enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerNotes disabled.");
    }

    public void reloadPlugin() {
        configManager.reload();
        messageService.reload();
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public MessageService messages() {
        return messageService;
    }

    private void registerCommands() {
        PluginCommand command = getCommand("pn");
        if (command == null) {
            getLogger().severe("Command 'pn' is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        List<SubCommand> subCommands = List.of(
                new VersionSubCommand(this, messageService),
                new ReloadSubCommand(this, messageService)
        );

        PlayerNotesCommand executor = new PlayerNotesCommand(messageService, subCommands);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void logSevere(String message, Throwable throwable) {
        getLogger().log(Level.SEVERE, message, throwable);
    }
}
