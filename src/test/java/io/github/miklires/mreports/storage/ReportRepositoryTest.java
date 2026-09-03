package io.github.miklires.mreports.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.miklires.mreports.api.ReportStatus;
import io.github.miklires.mreports.api.ReportPriority;
import io.github.miklires.mreports.evidence.EvidenceView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportRepositoryTest {
    @Test void submissionGetsStableIdAndDuplicateIsMerged() {
        try (ReportRepository repository = new ReportRepository("jdbc:h2:mem:submit;DB_CLOSE_DELAY=-1")) {
            UUID reporter = UUID.randomUUID(), target = UUID.randomUUID();
            var first = repository.submit(reporter, "Reporter", target, "Target", "CHEATING", "first", 60_000).join();
            var second = repository.submit(reporter, "Reporter", target, "Target", "CHEATING", "more", 60_000).join();
            assertFalse(first.merged()); assertTrue(second.merged());
            assertEquals(first.report().id(), second.report().id());
            assertEquals(2, second.report().duplicateCount());
            assertEquals("more", second.report().details());
        }
    }

    @Test void supportsPrioritySearchHistoryAndLongDetails() {
        try (ReportRepository repository = new ReportRepository("jdbc:h2:mem:workflow;DB_CLOSE_DELAY=-1")) {
            UUID reporter = UUID.randomUUID(), target = UUID.randomUUID(), moderator = UUID.randomUUID();
            String details = "x".repeat(1200);
            long id = repository.submit(reporter, "Reporter", target, "Target", "CHEATING", details, 0).join().report().id();
            assertTrue(repository.setPriority(id, moderator, "Mod", ReportPriority.URGENT).join());
            assertEquals(ReportPriority.URGENT, repository.find(id).join().orElseThrow().priority());
            assertEquals(id, repository.search("target", 10).join().getFirst().id());
            assertEquals(id, repository.history(reporter, 10).join().getFirst().id());
            assertTrue(repository.claim(id, moderator, "Mod").join());
            assertTrue(repository.release(id, moderator, "Mod").join());
            assertEquals(ReportStatus.OPEN, repository.find(id).join().orElseThrow().status());
            assertEquals(1, repository.addEvidence(id, List.of(new EvidenceView(target, "Target", "recent message", Instant.EPOCH))).join());
            assertEquals("recent message", repository.evidence(id, 10).join().getFirst().body());
        }
    }

    @Test void onlyOneHandlerCanClaimAndCloseAReport() {
        try (ReportRepository repository = new ReportRepository("jdbc:h2:mem:claim;DB_CLOSE_DELAY=-1")) {
            UUID reporter = UUID.randomUUID(), target = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
            long id = repository.submit(reporter, "Reporter", target, "Target", "CHAT", "spam", 60_000).join().report().id();
            assertTrue(repository.claim(id, first, "Mod1").join());
            assertFalse(repository.claim(id, second, "Mod2").join());
            assertFalse(repository.close(id, second, "Mod2", ReportStatus.RESOLVED, "no").join());
            assertTrue(repository.close(id, first, "Mod1", ReportStatus.RESOLVED, "checked").join());
            assertEquals(ReportStatus.RESOLVED, repository.find(id).join().orElseThrow().status());
            assertEquals(0, repository.queue(10).join().size());
        }
    }
}
