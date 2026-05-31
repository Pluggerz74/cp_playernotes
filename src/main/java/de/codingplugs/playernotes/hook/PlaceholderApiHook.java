package de.codingplugs.playernotes.hook;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.model.NotePriority;
import de.codingplugs.playernotes.model.PlayerNote;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceholderApiHook extends PlaceholderExpansion {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final String LOADING = "loading";

    private final PlayerNotesPlugin plugin;
    private final Map<UUID, CachedSnapshot> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> refreshing = new ConcurrentHashMap<>();

    public PlaceholderApiHook(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playernotes";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        UUID uuid = player.getUniqueId();
        CachedSnapshot snapshot = cache.get(uuid);

        if (snapshot != null && !snapshot.isExpired()) {
            return snapshot.resolve(params);
        }

        requestRefresh(uuid);
        return LOADING;
    }

    public void clearCache() {
        cache.clear();
        refreshing.clear();
    }

    private void requestRefresh(UUID uuid) {
        if (refreshing.putIfAbsent(uuid, Boolean.TRUE) != null) {
            return;
        }

        CompletableFuture<Integer> activeCount = plugin.notes().countActiveNotes(uuid);
        CompletableFuture<Integer> criticalCount = plugin.notes().countCriticalNotes(uuid);
        CompletableFuture<Integer> highRiskCount = plugin.notes().countActiveNotesAtOrAbovePriority(uuid, NotePriority.HIGH);
        CompletableFuture<List<PlayerNote>> notes = plugin.notes().findByTarget(uuid, false);

        CompletableFuture.allOf(activeCount, criticalCount, highRiskCount, notes).whenComplete((ignored, error) -> {
            refreshing.remove(uuid);

            if (error != null) {
                plugin.logSevere("Failed to refresh PlaceholderAPI cache for " + uuid, error);
                return;
            }

            cache.put(uuid, CachedSnapshot.from(
                    activeCount.join(),
                    criticalCount.join(),
                    highRiskCount.join(),
                    formatLatestNote(notes.join())
            ));
        });
    }

    private static String formatLatestNote(List<PlayerNote> notes) {
        if (notes.isEmpty()) {
            return "none";
        }

        String content = notes.get(0).getContent();
        if (content.length() <= 32) {
            return content;
        }

        return content.substring(0, 29) + "...";
    }

    private record CachedSnapshot(
            int activeCount,
            int criticalCount,
            int highRiskCount,
            String latestNote,
            Instant cachedAt
    ) {
        private static CachedSnapshot from(int activeCount, int criticalCount, int highRiskCount, String latestNote) {
            return new CachedSnapshot(activeCount, criticalCount, highRiskCount, latestNote, Instant.now());
        }

        private boolean isExpired() {
            return Instant.now().isAfter(cachedAt.plus(CACHE_TTL));
        }

        private String resolve(String params) {
            return switch (params.toLowerCase(Locale.ROOT)) {
                case "active_count" -> String.valueOf(activeCount);
                case "critical_count" -> String.valueOf(criticalCount);
                case "high_risk_count" -> String.valueOf(highRiskCount);
                case "flagged" -> highRiskCount > 0 ? "yes" : "no";
                case "latest_note" -> latestNote;
                default -> null;
            };
        }
    }
}
