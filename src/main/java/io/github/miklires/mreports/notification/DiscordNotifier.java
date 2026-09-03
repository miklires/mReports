package io.github.miklires.mreports.notification;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.api.ReportView;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Semaphore;

public final class DiscordNotifier {
    private final MReportsPlugin plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Semaphore pending = new Semaphore(32);

    public DiscordNotifier(MReportsPlugin plugin) { this.plugin = plugin; }

    public void send(String action, ReportView report) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        URI webhook = SafeWebhookUrl.parse(plugin.getConfig().getString("discord.webhook-url", ""));
        if (webhook == null) return;
        if (!pending.tryAcquire()) { plugin.getLogger().warning("Discord notification queue is full; dropping an update"); return; }
        String content = "[mReports] " + action + " #" + report.id() + " | " + report.reporterName() + " -> "
                + report.targetName() + " | " + report.category() + " | " + report.status() + " | " + report.priority();
        String payload = "{\"content\":\"" + escape(content) + "\",\"allowed_mentions\":{\"parse\":[]}}";
        HttpRequest request = HttpRequest.newBuilder(webhook).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).orTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .whenComplete((response, error) -> {
                    pending.release();
                    if (error != null) plugin.getLogger().warning("Discord notification failed: " + error.getClass().getSimpleName());
                    else if (response.statusCode() < 200 || response.statusCode() >= 300)
                        plugin.getLogger().warning("Discord notification returned HTTP " + response.statusCode());
                });
    }

    private static String escape(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> result.append("\\\\"); case '"' -> result.append("\\\"");
                case '\n' -> result.append("\\n"); case '\r' -> result.append("\\r"); case '\t' -> result.append("\\t");
                default -> { if (character >= 0x20) result.append(character); }
            }
        }
        return result.toString();
    }
}
