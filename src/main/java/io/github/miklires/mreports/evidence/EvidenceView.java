package io.github.miklires.mreports.evidence;

import java.time.Instant;
import java.util.UUID;

public record EvidenceView(UUID sourceId, String sourceName, String body, Instant occurredAt) {}
