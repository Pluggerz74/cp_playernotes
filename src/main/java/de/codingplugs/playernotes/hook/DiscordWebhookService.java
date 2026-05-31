package de.codingplugs.playernotes.hook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.codingplugs.playernotes.PlayerNotesPlugin;
import de.codingplugs.playernotes.model.PlayerNote;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class DiscordWebhookService {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final int COLOR_DEFAULT = 0x3498DB;
    private static final int COLOR_CRITICAL = 0xE74C3C;
    private static final int COLOR_NEUTRAL = 0x95A5A6;
    private static final int MAX_FIELD_LENGTH = 1024;
    private static final int MAX_TITLE_LENGTH = 256;
    private static final int MAX_USERNAME_LENGTH = 80;
    private static final String PLUGIN_DISPLAY_NAME = "PlayerNotes Pro";

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

    public CompletableFuture<WebhookResult> sendTest(String senderName) {
        if (!config().getBoolean("discord.enabled", false)) {
            plugin.getLogger().warning("[Discord] Webhook test skipped: discord.enabled is false.");
            return CompletableFuture.completedFuture(WebhookResult.failure(0, "Discord webhooks are disabled."));
        }

        String url = webhookUrl();
        if (url.isBlank()) {
            plugin.getLogger().warning("[Discord] Webhook test skipped: discord.webhook-url is empty.");
            return CompletableFuture.completedFuture(WebhookResult.failure(0, "Webhook URL is empty."));
        }

        if (!isValidDiscordWebhookUrl(url)) {
            plugin.getLogger().warning(
                    "[Discord] Webhook test skipped: discord.webhook-url must start with "
                            + "https://discord.com/api/webhooks/ or https://discordapp.com/api/webhooks/"
            );
            return CompletableFuture.completedFuture(WebhookResult.failure(0, "Invalid Discord webhook URL."));
        }

        String payload = buildPayload("Webhook Test", COLOR_DEFAULT, List.of(
                field("Plugin", PLUGIN_DISPLAY_NAME),
                field("Sender", senderName),
                field("Server", plugin.getServer().getName())
        ));

        return sendPayload(url, payload);
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
                    field("Content", note.getContent(), false)
            ));
            return;
        }

        if (notify("note-created")) {
            sendEmbed("Note Created", COLOR_DEFAULT, List.of(
                    field("Player", note.getTargetName()),
                    field("Staff", note.getStaffName()),
                    field("Type", note.getType().name()),
                    field("Priority", note.getPriority().name()),
                    field("Content", note.getContent(), false)
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
        String url = webhookUrl();
        if (url.isBlank()) {
            return;
        }

        String payload = buildPayload(title, color, fields);

        sendPayload(url, payload).whenComplete((result, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to send Discord webhook", error);
            }
        });
    }

    private CompletableFuture<WebhookResult> sendPayload(String webhookUrl, String payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        return WebhookResult.failure(0, error.getMessage());
                    }

                    int status = response.statusCode();
                    if (status >= 200 && status < 300) {
                        return WebhookResult.success(status);
                    }

                    String body = response.body();
                    plugin.getLogger().log(
                            Level.WARNING,
                            "Discord webhook returned HTTP " + status + ": " + body
                    );
                    return WebhookResult.httpFailure(status, body);
                });
    }

    private String buildPayload(String title, int color, List<EmbedField> fields) {
        JsonObject root = new JsonObject();
        root.addProperty("username", truncate(username(), MAX_USERNAME_LENGTH));

        String avatar = avatarUrl();
        if (!avatar.isBlank()) {
            root.addProperty("avatar_url", avatar);
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("title", truncate(title, MAX_TITLE_LENGTH));
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());

        JsonArray fieldsArray = new JsonArray();
        for (EmbedField embedField : fields) {
            if (embedField.name() == null || embedField.name().isBlank()) {
                continue;
            }
            if (embedField.value() == null || embedField.value().isBlank()) {
                continue;
            }

            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty("name", truncate(embedField.name(), MAX_FIELD_LENGTH));
            fieldObject.addProperty("value", truncate(embedField.value(), MAX_FIELD_LENGTH));
            fieldObject.addProperty("inline", embedField.inline());
            fieldsArray.add(fieldObject);
        }

        embed.add("fields", fieldsArray);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        root.add("embeds", embeds);

        String payload = GSON.toJson(root);
        logPayload(payload);
        return payload;
    }

    private void logPayload(String payload) {
        if (config().getBoolean("discord.debug-payload", false)) {
            plugin.getLogger().info("[Discord] Webhook payload: " + payload);
            return;
        }

        if (config().getBoolean("debug", false)) {
            plugin.getLogger().log(Level.FINE, "[Discord] Webhook payload: " + payload);
        }
    }

    private static EmbedField field(String name, String value) {
        return field(name, value, true);
    }

    private static EmbedField field(String name, String value, boolean inline) {
        return new EmbedField(name, value, inline);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
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
        String configured = config().getString("discord.username", PLUGIN_DISPLAY_NAME);
        return configured == null || configured.isBlank() ? PLUGIN_DISPLAY_NAME : configured;
    }

    private static boolean isValidDiscordWebhookUrl(String url) {
        return url.startsWith("https://discord.com/api/webhooks/")
                || url.startsWith("https://discordapp.com/api/webhooks/");
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
