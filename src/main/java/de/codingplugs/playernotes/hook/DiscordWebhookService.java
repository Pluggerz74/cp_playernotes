package de.codingplugs.playernotes.hook;

import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.model.PlayerNote;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;

public final class DiscordWebhookService {

    private static final int COLOR_DEFAULT = 0x3498DB;
    private static final int COLOR_CRITICAL = 0xE74C3C;
    private static final int COLOR_NEUTRAL = 0x95A5A6;
    private static final int MAX_FIELD_LENGTH = 1024;

    private final PlayerNotesPlugin plugin;
    private final HttpClient httpClient;

    public DiscordWebhookService(PlayerNotesPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void reload() {
        // Configuration is read on each request so reload requires no extra work.
    }

    public void notifyNoteCreated(PlayerNote note) {
        if (!isActive() || note == null) {
            return;
        }

        if (note.isCritical() && notify("critical-note-created")) {
            sendEmbed("Critical Note Created", COLOR_CRITICAL, List.of(
                    field("Player", note.getTargetName()),
                    field("Staff", note.getStaffName()),
                    field("Type", note.getType().name()),
                    field("Priority", note.getPriority().name()),
                    field("Content", truncate(note.getContent()), false)
            ));
            return;
        }

        if (notify("note-created")) {
            sendEmbed("Note Created", COLOR_DEFAULT, List.of(
                    field("Player", note.getTargetName()),
                    field("Staff", note.getStaffName()),
                    field("Type", note.getType().name()),
                    field("Priority", note.getPriority().name()),
                    field("Content", truncate(note.getContent()), false)
            ));
        }
    }

    public void notifyNoteArchived(long noteId, String staffName) {
        if (!isActive() || !notify("note-archived")) {
            return;
        }

        sendEmbed("Note Archived", COLOR_NEUTRAL, List.of(
                field("Note ID", String.valueOf(noteId)),
                field("Staff", staffName)
        ));
    }

    public void notifyNoteDeleted(long noteId, String staffName) {
        if (!isActive() || !notify("note-deleted")) {
            return;
        }

        sendEmbed("Note Deleted", COLOR_NEUTRAL, List.of(
                field("Note ID", String.valueOf(noteId)),
                field("Staff", staffName)
        ));
    }

    public void notifyFlaggedPlayerJoin(String playerName, int highRiskNotes) {
        if (!isActive() || !notify("flagged-player-join")) {
            return;
        }

        sendEmbed("Flagged Player Joined", COLOR_CRITICAL, List.of(
                field("Player", playerName),
                field("High-risk notes", String.valueOf(highRiskNotes))
        ));
    }

    private void sendEmbed(String title, int color, List<EmbedField> fields) {
        String webhookUrl = webhookUrl();
        if (webhookUrl.isBlank()) {
            return;
        }

        String payload = buildPayload(title, color, fields);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Failed to send Discord webhook", error);
                        return;
                    }

                    int status = response.statusCode();
                    if (status < 200 || status >= 300) {
                        plugin.getLogger().log(
                                Level.WARNING,
                                "Discord webhook returned HTTP " + status + ": " + response.body()
                        );
                    }
                });
    }

    private String buildPayload(String title, int color, List<EmbedField> fields) {
        StringBuilder json = new StringBuilder(256);
        json.append('{');

        appendJsonString(json, "username", username());
        String avatarUrl = avatarUrl();
        if (!avatarUrl.isBlank()) {
            json.append(',');
            appendJsonString(json, "avatar_url", avatarUrl);
        }

        json.append(",\"embeds\":[{");
        appendJsonString(json, "title", title);
        json.append(",\"color\":").append(color);
        json.append(",\"fields\":[");

        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                json.append(',');
            }

            EmbedField embedField = fields.get(index);
            json.append('{');
            appendJsonString(json, "name", embedField.name());
            appendJsonString(json, "value", embedField.value());
            json.append(",\"inline\":").append(embedField.inline());
            json.append('}');
        }

        json.append("]}]}");
        return json.toString();
    }

    private static EmbedField field(String name, String value) {
        return field(name, value, true);
    }

    private static EmbedField field(String name, String value, boolean inline) {
        return new EmbedField(name, value, inline);
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }

        if (value.length() <= MAX_FIELD_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_FIELD_LENGTH - 3) + "...";
    }

    private static void appendJsonString(StringBuilder json, String key, String value) {
        json.append('"').append(escapeJson(key)).append("\":\"").append(escapeJson(value)).append('"');
    }

    private static String escapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private boolean isActive() {
        return config().getBoolean("discord.enabled", false) && !webhookUrl().isBlank();
    }

    private boolean notify(String key) {
        return config().getBoolean("discord.notify." + key, true);
    }

    private String webhookUrl() {
        return config().getString("discord.webhook-url", "").trim();
    }

    private String username() {
        String configured = config().getString("discord.username", "PlayerNotes Pro");
        return configured == null || configured.isBlank() ? "PlayerNotes Pro" : configured;
    }

    private String avatarUrl() {
        return config().getString("discord.avatar-url", "").trim();
    }

    private FileConfiguration config() {
        return plugin.configManager().config();
    }

    private record EmbedField(String name, String value, boolean inline) {
    }
}
