package io.github.miklires.mreports.api;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
public interface MReportsApi {
    CompletableFuture<ReportView> submit(UUID reporterId, String reporterName, UUID targetId, String targetName, String category, String details);
    CompletableFuture<Optional<ReportView>> find(long id);
    CompletableFuture<List<ReportView>> openReports(int limit);
    CompletableFuture<List<ReportView>> search(String query, int limit);
    CompletableFuture<List<ReportView>> history(UUID playerId, int limit);
}
