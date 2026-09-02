package io.github.miklires.mreports.report;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class ReportPolicyTest {
    @Test void blocksSelfExemptAndRepeatedReports() {
        UUID reporter = UUID.randomUUID(), target = UUID.randomUUID();
        ReportPolicy policy = new ReportPolicy(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), Duration.ofSeconds(30));
        assertEquals(ReportPolicy.Result.SELF_REPORT, policy.validate(reporter, reporter, false));
        assertEquals(ReportPolicy.Result.TARGET_EXEMPT, policy.validate(reporter, target, true));
        assertEquals(ReportPolicy.Result.ALLOWED, policy.validate(reporter, target, false));
        assertEquals(ReportPolicy.Result.COOLDOWN, policy.validate(reporter, target, false));
    }
    @Test void enforcesRollingLimitAndAllowsExplicitBypass() {
        UUID reporter = UUID.randomUUID(), target = UUID.randomUUID();
        ReportPolicy policy = new ReportPolicy(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), Duration.ZERO, Duration.ofMinutes(10), 2, 100);
        assertEquals(ReportPolicy.Result.ALLOWED, policy.validate(reporter, target, false));
        assertEquals(ReportPolicy.Result.ALLOWED, policy.validate(reporter, UUID.randomUUID(), false));
        assertEquals(ReportPolicy.Result.RATE_LIMIT, policy.validate(reporter, UUID.randomUUID(), false));
        assertEquals(ReportPolicy.Result.ALLOWED, policy.validate(reporter, UUID.randomUUID(), false, true));
    }
}
