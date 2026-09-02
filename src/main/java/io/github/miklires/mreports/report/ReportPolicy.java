package io.github.miklires.mreports.report;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReportPolicy {
    private final Clock clock;
    private final Duration cooldown;
    private final Duration rateWindow;
    private final int maximumReports;
    private final int maximumTrackedPlayers;
    private final Map<UUID, Window> submissions = new ConcurrentHashMap<>();
    public ReportPolicy(Clock clock, Duration cooldown) { this(clock, cooldown, Duration.ofMinutes(10), 5, 10_000); }
    public ReportPolicy(Clock clock, Duration cooldown, Duration rateWindow, int maximumReports, int maximumTrackedPlayers) {
        this.clock = clock; this.cooldown = cooldown; this.rateWindow = rateWindow;
        this.maximumReports = maximumReports; this.maximumTrackedPlayers = maximumTrackedPlayers;
    }
    public Result validate(UUID reporter, UUID target, boolean exempt, boolean bypassLimits) {
        if (reporter.equals(target)) return Result.SELF_REPORT;
        if (exempt) return Result.TARGET_EXEMPT;
        if (bypassLimits) return Result.ALLOWED;
        Instant now = clock.instant();
        Window existing = submissions.get(reporter);
        if (existing == null && submissions.size() >= maximumTrackedPlayers) cleanup(now);
        if (existing == null && submissions.size() >= maximumTrackedPlayers) return Result.RATE_LIMIT;
        return submissions.computeIfAbsent(reporter, ignored -> new Window()).validate(now);
    }
    public Result validate(UUID reporter, UUID target, boolean exempt) { return validate(reporter, target, exempt, false); }
    private void cleanup(Instant now) {
        Instant cutoff = now.minus(rateWindow);
        submissions.entrySet().removeIf(entry -> entry.getValue().expired(cutoff));
    }
    private final class Window {
        private final ArrayDeque<Instant> attempts = new ArrayDeque<>();
        private synchronized Result validate(Instant now) {
            Instant cutoff = now.minus(rateWindow);
            while (!attempts.isEmpty() && !attempts.getFirst().isAfter(cutoff)) attempts.removeFirst();
            Instant previous = attempts.peekLast();
            if (previous != null && previous.plus(cooldown).isAfter(now)) return Result.COOLDOWN;
            if (attempts.size() >= maximumReports) return Result.RATE_LIMIT;
            attempts.addLast(now);
            return Result.ALLOWED;
        }
        private synchronized boolean expired(Instant cutoff) { return attempts.isEmpty() || !attempts.getLast().isAfter(cutoff); }
    }
    public enum Result { ALLOWED, SELF_REPORT, TARGET_EXEMPT, COOLDOWN, RATE_LIMIT }
}
