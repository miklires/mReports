package io.github.miklires.mreports.report;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.storage.ReportRepository;
import io.github.miklires.mreports.evidence.ChatEvidenceService;
import io.github.miklires.mreports.notification.DiscordNotifier;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ReportService {
    private static final long MAX_INTERVAL_SECONDS = 31_536_000L;
    private static final int MAX_DETAILS_LENGTH = 2_000;
    private final MReportsPlugin plugin;
    private final ReportRepository repository;
    private final ChatEvidenceService evidence;
    private final DiscordNotifier discord;
    private volatile ReportPolicy policy;
    private volatile List<String> categories;
    public ReportService(MReportsPlugin plugin, ReportRepository repository, ChatEvidenceService evidence) {
        this.plugin = plugin; this.repository = repository; this.evidence = evidence; this.discord = new DiscordNotifier(plugin); reload();
    }
    public void reload() {
        ReportPolicy loadedPolicy = new ReportPolicy(java.time.Clock.systemUTC(), java.time.Duration.ofSeconds(seconds("submission.cooldown-seconds", 30)),
                java.time.Duration.ofSeconds(seconds("submission.rate-limit.window-seconds", 600)),
                Math.clamp(plugin.getConfig().getInt("submission.rate-limit.maximum-reports", 5), 1, 1000),
                Math.clamp(plugin.getConfig().getInt("submission.rate-limit.maximum-tracked-players", 10_000), 100, 100_000));
        List<String> loadedCategories = plugin.getConfig().getStringList("submission.categories").stream()
                .map(String::trim).map(s -> s.toUpperCase(Locale.ROOT))
                .filter(s -> s.matches("[A-Z0-9_]{1,32}"))
                .limit(32)
                .distinct().toList();
        if (loadedCategories.isEmpty()) throw new IllegalArgumentException("At least one report category is required");
        policy = loadedPolicy;
        categories = loadedCategories;
    }
    public CompletableFuture<SubmitResult> submit(Player reporter, UUID targetId, String targetName, String category, String details, boolean exempt) {
        if (category == null || details == null || targetName == null) return CompletableFuture.completedFuture(new SubmitResult(null, Failure.INVALID_TEXT));
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!categories.contains(normalized)) return CompletableFuture.completedFuture(new SubmitResult(null, Failure.BAD_CATEGORY));
        int maximumDetails = Math.clamp(plugin.getConfig().getInt("submission.maximum-details-length", 300), 1, MAX_DETAILS_LENGTH);
        int minimumDetails = Math.clamp(plugin.getConfig().getInt("submission.minimum-details-length", 3), 1, maximumDetails);
        String cleanDetails = details.trim();
        if (cleanDetails.length() < minimumDetails) return CompletableFuture.completedFuture(new SubmitResult(null, Failure.TOO_SHORT));
        if (cleanDetails.length() > maximumDetails || cleanDetails.chars().anyMatch(Character::isISOControl))
            return CompletableFuture.completedFuture(new SubmitResult(null, Failure.INVALID_TEXT));
        ReportPolicy.Result decision = policy.validate(reporter.getUniqueId(), targetId, exempt, reporter.hasPermission("mreports.bypass.cooldown"));
        if (decision != ReportPolicy.Result.ALLOWED) return CompletableFuture.completedFuture(new SubmitResult(null, Failure.valueOf(decision.name())));
        long window = seconds("submission.duplicate-window-seconds", 300) * 1_000L;
        return repository.submit(reporter.getUniqueId(), reporter.getName(), targetId, targetName, normalized, cleanDetails, window)
                .thenCompose(submission -> repository.addEvidence(submission.report().id(), evidence.snapshot(targetId)).thenApply(ignored -> submission))
                .thenApply(submission -> {
                    notifyStaff(submission.report().id(), reporter.getName(), targetName, normalized, submission.merged());
                    discord.send(submission.merged() ? "updated" : "created", submission.report());
                    return new SubmitResult(submission, null);
                });
    }
    private void notifyStaff(long id, String reporter, String target, String category, boolean merged) {
        plugin.scheduler().global(() -> Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("mreports.notify")).forEach(player ->
                player.sendMessage(color("&c[Reports] &f#" + id + " &7" + reporter + " -> " + target + " [&e" + category + "&7]" + (merged ? text(" &6(updated)"," &6(обновлён)") : "")))));
    }
    public List<String> categories() { return categories; }
    public String text(String english,String russian){return io.github.miklires.mreports.Text.tr(plugin,english,russian);}
    private long seconds(String path, long fallback) {
        return Math.clamp(plugin.getConfig().getLong(path, fallback), 0L, MAX_INTERVAL_SECONDS);
    }
    public ReportRepository repository() { return repository; }
    public void notifyChange(String action, long id) { repository.find(id).thenAccept(found -> found.ifPresent(report -> discord.send(action, report))); }
    public String failure(Failure failure) {
        return switch (failure) {
            case SELF_REPORT -> text("You cannot report yourself.", "Нельзя отправить репорт на себя.");
            case TARGET_EXEMPT -> text("That player cannot be reported.", "На этого игрока нельзя отправить репорт.");
            case COOLDOWN -> text("Wait before sending another report.", "Подождите перед следующим репортом.");
            case RATE_LIMIT -> text("You reached the report limit for this time window.", "Вы достигли лимита репортов за этот период.");
            case BAD_CATEGORY -> text("Unknown report category.", "Неизвестная категория репорта.");
            case TOO_SHORT -> text("Add more useful details.", "Добавьте больше полезных деталей.");
            case INVALID_TEXT -> text("The report text is too long or contains invalid characters.", "Текст репорта слишком длинный или содержит недопустимые символы.");
        };
    }
    public static String color(String text) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', text); }
    public record SubmitResult(ReportRepository.Submission submission, Failure failure) { public boolean success() { return failure == null; } }
    public enum Failure { SELF_REPORT, TARGET_EXEMPT, COOLDOWN, RATE_LIMIT, BAD_CATEGORY, TOO_SHORT, INVALID_TEXT }
}
