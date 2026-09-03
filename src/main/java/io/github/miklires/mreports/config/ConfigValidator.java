package io.github.miklires.mreports.config;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.notification.SafeWebhookUrl;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigValidator {
    private static final List<String> DEFAULT_CATEGORIES = List.of("CHEATING", "CHAT", "TEAMING", "GRIEFING", "OTHER");
    private final MReportsPlugin plugin;
    public ConfigValidator(MReportsPlugin plugin) { this.plugin = plugin; }
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);
        config.set("config-version", 2);
        String language = config.getString("language", "en_US");
        if (!"en_US".equalsIgnoreCase(language) && !"ru_RU".equalsIgnoreCase(language)) config.set("language", "en_US");
        bounded(config, "submission.cooldown-seconds", 0, 31_536_000, 30);
        bounded(config, "submission.duplicate-window-seconds", 0, 31_536_000, 300);
        int maximum = bounded(config, "submission.maximum-details-length", 1, 2_000, 300);
        bounded(config, "submission.minimum-details-length", 1, maximum, Math.min(3, maximum));
        bounded(config, "submission.rate-limit.maximum-reports", 1, 1_000, 5);
        bounded(config, "submission.rate-limit.window-seconds", 1, 31_536_000, 600);
        bounded(config, "submission.rate-limit.maximum-tracked-players", 100, 100_000, 10_000);
        normalizeCategories(config);
        bounded(config, "storage.retention-days", 1, 36_500, 90);
        bounded(config, "evidence.chat.retention-seconds", 30, 86_400, 300);
        bounded(config, "evidence.chat.maximum-messages-per-player", 1, 50, 10);
        bounded(config, "evidence.chat.maximum-message-length", 16, 1_000, 256);
        bounded(config, "evidence.chat.maximum-tracked-players", 100, 100_000, 10_000);
        String project = config.getString("updates.modrinth-project-id", "ZRybXlNb");
        if (project == null || !project.matches("[A-Za-z0-9]{3,64}")) config.set("updates.modrinth-project-id", "ZRybXlNb");
        if (config.getBoolean("discord.enabled") && SafeWebhookUrl.parse(config.getString("discord.webhook-url", "")) == null) {
            config.set("discord.enabled", false);
            plugin.getLogger().warning("Discord notifications were disabled because webhook-url is not a valid Discord HTTPS webhook");
        }
        plugin.saveConfig();
    }
    private int bounded(FileConfiguration config, String path, int minimum, int maximum, int fallback) {
        int value = config.getInt(path, fallback);
        if (value < minimum || value > maximum) { config.set(path, fallback); return fallback; }
        return value;
    }
    private void normalizeCategories(FileConfiguration config) {
        Set<String> categories = new LinkedHashSet<>();
        for (String raw : config.getStringList("submission.categories")) {
            String category = raw.strip().toUpperCase(Locale.ROOT);
            if (category.matches("[A-Z0-9_]{1,32}")) categories.add(category);
            if (categories.size() == 32) break;
        }
        config.set("submission.categories", categories.isEmpty() ? DEFAULT_CATEGORIES : List.copyOf(categories));
    }
}
