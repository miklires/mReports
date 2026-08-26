package io.github.miklires.mreports.api;
import java.time.Instant;
import java.util.UUID;
public record ReportView(long id, UUID reporterId, String reporterName, UUID targetId, String targetName,
                         String category, String details, ReportStatus status, UUID handlerId,
                         String handlerName, int duplicateCount, Instant createdAt, Instant updatedAt) {}
