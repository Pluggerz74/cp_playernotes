package de.codingplugs.playernotes;

import de.codingplugs.playernotes.command.AddSubCommand;
import de.codingplugs.playernotes.command.ArchiveSubCommand;
import de.codingplugs.playernotes.command.ListSubCommand;
import de.codingplugs.playernotes.command.PlayerNotesCommand;
import de.codingplugs.playernotes.command.ReloadSubCommand;
import de.codingplugs.playernotes.command.RemoveSubCommand;
import de.codingplugs.playernotes.command.SubCommand;
import de.codingplugs.playernotes.command.VersionSubCommand;
import de.codingplugs.playernotes.command.ViewPlayerCommand;
import de.codingplugs.playernotes.config.ConfigManager;
import de.codingplugs.playernotes.database.DatabaseProvider;
import de.codingplugs.playernotes.database.NoteRepository;
import de.codingplugs.playernotes.database.SQLiteDatabaseProvider;
import de.codingplugs.playernotes.database.SqlNoteRepository;
import de.codingplugs.playernotes.gui.GuiClickListener;
import de.codingplugs.playernotes.gui.GuiManager;
import de.codingplugs.playernotes.hook.DiscordWebhookService;
import de.codingplugs.playernotes.hook.HookManager;
import de.codingplugs.playernotes.listener.ChatInputListener;
import de.codingplugs.playernotes.listener.StaffJoinListener;
import de.codingplugs.playernotes.service.ChatInputService;
import de.codingplugs.playernotes.service.JoinAlertService;
import de.codingplugs.playernotes.service.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public final class PlayerNotesPlugin extends JavaPlugin {

    public static final String SUPPORTED_SERVER_VERSIONS = "1.19.4 - 1.21.x";

    private ConfigManager configManager;
    private MessageService messageService;
    private DatabaseProvider databaseProvider;
    private NoteRepository noteRepository;
    private GuiManager guiManager;
    private ChatInputService chatInputService;
    private HookManager hookManager;
    private DiscordWebhookService discordWebhookService;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        if (!configManager.load()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        messageService = new MessageService(configManager);
        messageService.load();
        discordWebhookService = new DiscordWebhookService(this);

        if (!initializeDatabase()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerListeners();
        registerCommands();
        registerHooks();

        getLogger().info("PlayerNotes enabled.");
    }

    @Override
    public void onDisable() {
        if (hookManager != null) {
            hookManager.shutdown();
        }
        if (chatInputService != null) {
            chatInputService.shutdown();
        }
        shutdownDatabase();
        getLogger().info("PlayerNotes disabled.");
    }

    public void reloadPlugin() {
        configManager.reload();
        messageService.reload();
        if (hookManager != null) {
            hookManager.reload();
        }
        if (discordWebhookService != null) {
            discordWebhookService.reload();
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public MessageService messages() {
        return messageService;
    }

    public NoteRepository notes() {
        return noteRepository;
    }

    public GuiManager guis() {
        return guiManager;
    }

    public ChatInputService chatInput() {
        return chatInputService;
    }

    public DiscordWebhookService discord() {
        return discordWebhookService;
    }

    private boolean initializeDatabase() {
        databaseProvider = new SQLiteDatabaseProvider(this);
        try {
            databaseProvider.initialize();
        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, "Failed to initialize SQLite database", exception);
            return false;
        }

        noteRepository = new SqlNoteRepository(databaseProvider);
        return true;
    }

    private void shutdownDatabase() {
        if (databaseProvider != null) {
            databaseProvider.shutdown();
            databaseProvider = null;
            noteRepository = null;
        }
    }

    private void registerCommands() {
        PluginCommand command = getCommand("pn");
        if (command == null) {
            getLogger().severe("Command 'pn' is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        List<SubCommand> subCommands = List.of(
                new AddSubCommand(this, messageService),
                new ListSubCommand(this, messageService),
                new ArchiveSubCommand(this, messageService),
                new RemoveSubCommand(this, messageService),
                new VersionSubCommand(this, messageService),
                new ReloadSubCommand(this, messageService)
        );

        ViewPlayerCommand viewPlayerCommand = new ViewPlayerCommand(this, messageService, guiManager);
        PlayerNotesCommand executor = new PlayerNotesCommand(messageService, subCommands, viewPlayerCommand);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        guiManager = new GuiManager(this);
        chatInputService = new ChatInputService(this, guiManager);
        JoinAlertService joinAlertService = new JoinAlertService(this);

        getServer().getPluginManager().registerEvents(new GuiClickListener(guiManager, chatInputService), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this, chatInputService), this);
        getServer().getPluginManager().registerEvents(new StaffJoinListener(joinAlertService), this);
    }

    private void registerHooks() {
        hookManager = new HookManager(this);
        hookManager.registerHooks();
    }

    public void logSevere(String message, Throwable throwable) {
        getLogger().log(Level.SEVERE, message, throwable);
    }
}
