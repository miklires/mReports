package io.github.miklires.mreports.report;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.storage.ReportRepository;
import java.util.Locale;
import java.util.Set;
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
    private volatile Set<String> categories;
    public ReportService(MReportsPlugin plugin, ReportRepository repository) { this.plugin = plugin; this.repository = repository; reload(); }
    public void reload() {
        policy = new ReportPolicy(java.time.Clock.systemUTC(), java.time.Duration.ofSeconds(seconds("submission.cooldown-seconds", 30)));
        categories = plugin.getConfig().getStringList("submission.categories").stream()
                .map(String::trim).map(s -> s.toUpperCase(Locale.ROOT))
                .filter(s -> s.matches("[A-Z0-9_]{1,32}"))
                .limit(32)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (categories.isEmpty()) throw new IllegalArgumentException("At least one report category is required");
    }
    public CompletableFuture<SubmitResult> submit(Player reporter, UUID targetId, String targetName, String category, String details, boolean exempt) {
        String normalized = category.toUpperCase(Locale.ROOT);
        if (!categories.contains(normalized)) return CompletableFuture.completedFuture(new SubmitResult(null, Failure.BAD_CATEGORY));
        int maximumDetails = Math.clamp(plugin.getConfig().getInt("submission.maximum-details-length", 300), 1, MAX_DETAILS_LENGTH);
        if (details.isBlank() || details.length() > maximumDetails)
            return CompletableFuture.completedFuture(new SubmitResult(null, Failure.TOO_LONG));
        ReportPolicy.Result decision = policy.validate(reporter.getUniqueId(), targetId, exempt);
        if (decision != ReportPolicy.Result.ALLOWED) return CompletableFuture.completedFuture(new SubmitResult(null, Failure.valueOf(decision.name())));
        long window = seconds("submission.duplicate-window-seconds", 300) * 1_000L;
        return repository.submit(reporter.getUniqueId(), reporter.getName(), targetId, targetName, normalized, details, window)
                .thenApply(submission -> {
                    notifyStaff(submission.report().id(), reporter.getName(), targetName, normalized, submission.merged());
                    return new SubmitResult(submission, null);
                });
    }
    private void notifyStaff(long id, String reporter, String target, String category, boolean merged) {
        plugin.scheduler().global(() -> Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("mreports.notify")).forEach(player ->
                player.sendMessage(color("&c[Reports] &f#" + id + " &7" + reporter + " -> " + target + " [&e" + category + "&7]" + (merged ? text(" &6(updated)"," &6(обновлён)") : "")))));
    }
    public Set<String> categories() { return categories; }
    public String text(String english,String russian){return io.github.miklires.mreports.Text.tr(plugin,english,russian);}
    private long seconds(String path, long fallback) {
        return Math.clamp(plugin.getConfig().getLong(path, fallback), 0L, MAX_INTERVAL_SECONDS);
    }
    public ReportRepository repository() { return repository; }
    public static String color(String text) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', text); }
    public record SubmitResult(ReportRepository.Submission submission, Failure failure) { public boolean success() { return failure == null; } }
    public enum Failure { SELF_REPORT, TARGET_EXEMPT, COOLDOWN, BAD_CATEGORY, TOO_LONG }
}
