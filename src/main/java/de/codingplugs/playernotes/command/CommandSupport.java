package de.codingplugs.playernotes.command;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.permission.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CommandSupport {

    public static final UUID CONSOLE_UUID = new UUID(0L, 0L);
    public static final String CONSOLE_NAME = "Console";

    private CommandSupport() {
    }

    public static boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(Permissions.ADMIN) || sender.hasPermission(permission);
    }

    public static void runSync(PlayerNotesPlugin plugin, Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public static Optional<OfflinePlayer> resolvePlayer(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return Optional.of(online);
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return Optional.of(offline);
        }

        return Optional.empty();
    }

    public static String displayName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name != null && !name.isBlank() ? name : fallback;
    }

    public static StaffIdentity staffIdentity(CommandSender sender) {
        if (sender instanceof Player player) {
            return new StaffIdentity(player.getUniqueId(), player.getName());
        }
        return new StaffIdentity(CONSOLE_UUID, CONSOLE_NAME);
    }

    public static Optional<Long> parseNoteId(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        try {
            long id = Long.parseLong(input);
            if (id <= 0) {
                return Optional.empty();
            }
            return Optional.of(id);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static String joinArgs(String[] args, int startIndex) {
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
    }

    public static List<String> tabCompleteOnlinePlayers(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public record StaffIdentity(UUID uuid, String name) {
    }
}
