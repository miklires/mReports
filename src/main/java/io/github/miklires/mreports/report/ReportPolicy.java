package io.github.miklires.mreports.report;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReportPolicy {
    private final Clock clock;
    private final Duration cooldown;
    private final Map<UUID, Instant> lastSubmission = new ConcurrentHashMap<>();
    public ReportPolicy(Clock clock, Duration cooldown) { this.clock = clock; this.cooldown = cooldown; }
    public Result validate(UUID reporter, UUID target, boolean exempt) {
        if (reporter.equals(target)) return Result.SELF_REPORT;
        if (exempt) return Result.TARGET_EXEMPT;
        Instant now = clock.instant();
        Instant previous = lastSubmission.get(reporter);
        if (previous != null && previous.plus(cooldown).isAfter(now)) return Result.COOLDOWN;
        lastSubmission.put(reporter, now);
        return Result.ALLOWED;
    }
    public enum Result { ALLOWED, SELF_REPORT, TARGET_EXEMPT, COOLDOWN }
}
