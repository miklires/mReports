package io.github.miklires.mreports.update;

import io.github.miklires.mreports.MReportsPlugin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final Pattern VERSION = Pattern.compile("\\\"version_number\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final MReportsPlugin plugin;
    public UpdateChecker(MReportsPlugin plugin) { this.plugin = plugin; }
    public void start() {
        if (plugin.getConfig().getBoolean("updates.enabled", true)) plugin.scheduler().async(this::check);
    }
    private void check() {
        String project = plugin.getConfig().getString("updates.modrinth-project-id", "ZRybXlNb");
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            URI uri = URI.create("https://api.modrinth.com/v2/project/" + project + "/version?loaders=%5B%22paper%22%5D&game_versions=%5B%2226.2%22%5D");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "miklires/mReports/" + plugin.getPluginMeta().getVersion()).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            Matcher matcher = VERSION.matcher(response.body());
            if (!matcher.find()) return;
            String available = matcher.group(1);
            if (SemanticVersion.parse(available).compareTo(SemanticVersion.parse(plugin.getPluginMeta().getVersion())) > 0)
                plugin.getLogger().info("A newer mReports version is available: " + available + " (Modrinth)");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            plugin.getLogger().fine("Update check skipped: " + exception.getMessage());
        }
    }
}
