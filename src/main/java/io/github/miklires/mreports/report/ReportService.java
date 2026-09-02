package io.github.miklires.mreports.report;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.storage.ReportRepository;
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
    private volatile ReportPolicy policy;
    private volatile List<String> categories;
    public ReportService(MReportsPlugin plugin, ReportRepository repository) { this.plugin = plugin; this.repository = repository; reload(); }
    public void reload() {
        policy = new ReportPolicy(java.time.Clock.systemUTC(), java.time.Duration.ofSeconds(seconds("submission.cooldown-seconds", 30)),
                java.time.Duration.ofSeconds(seconds("submission.rate-limit.window-seconds", 600)),
                Math.clamp(plugin.getConfig().getInt("submission.rate-limit.maximum-reports", 5), 1, 1000),
                Math.clamp(plugin.getConfig().getInt("submission.rate-limit.maximum-tracked-players", 10_000), 100, 100_000));
        categories = plugin.getConfig().getStringList("submission.categories").stream()
                .map(String::trim).map(s -> s.toUpperCase(Locale.ROOT))
                .filter(s -> s.matches("[A-Z0-9_]{1,32}"))
                .limit(32)
                .distinct().toList();
        if (categories.isEmpty()) throw new IllegalArgumentException("At least one report category is required");
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
                .thenApply(submission -> {
                    notifyStaff(submission.report().id(), reporter.getName(), targetName, normalized, submission.merged());
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
    public static String color(String text) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', text); }
    public record SubmitResult(ReportRepository.Submission submission, Failure failure) { public boolean success() { return failure == null; } }
    public enum Failure { SELF_REPORT, TARGET_EXEMPT, COOLDOWN, RATE_LIMIT, BAD_CATEGORY, TOO_SHORT, INVALID_TEXT }
}
