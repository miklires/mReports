package io.github.miklires.mreports;

import io.github.miklires.mreports.api.MReportsApi;
import io.github.miklires.mreports.command.MyReportsCommand;
import io.github.miklires.mreports.command.ReportCommand;
import io.github.miklires.mreports.command.ReportsCommand;
import io.github.miklires.mreports.config.ConfigValidator;
import io.github.miklires.mreports.gui.ReportGui;
import io.github.miklires.mreports.gui.StaffQueueGui;
import io.github.miklires.mreports.evidence.ChatEvidenceService;
import io.github.miklires.mreports.report.ReportService;
import io.github.miklires.mreports.storage.ReportRepository;
import io.github.miklires.mreports.util.PluginScheduler;
import io.github.miklires.mreports.update.UpdateChecker;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class MReportsPlugin extends JavaPlugin {
    private PluginScheduler scheduler;
    private ConfigValidator configValidator;
    private ReportRepository repository;
    private ReportService service;

    @Override public void onEnable() {
        scheduler = new PluginScheduler(this);
        configValidator = new ConfigValidator(this);
        configValidator.load();
        try {
            repository = new ReportRepository(getDataFolder().toPath().resolve("data").resolve("mreports"));
            ChatEvidenceService evidence = new ChatEvidenceService(this);
            service = new ReportService(this, repository, evidence);
            ReportGui reportGui = new ReportGui(this, service);
            StaffQueueGui queueGui = new StaffQueueGui(this, service);
            getServer().getPluginManager().registerEvents(reportGui, this);
            getServer().getPluginManager().registerEvents(queueGui, this);
            getServer().getPluginManager().registerEvents(evidence, this);
            getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                event.registrar().register("report", "Submit a player report", List.of(), new ReportCommand(service, reportGui));
                event.registrar().register("reports", "Moderation report queue", List.of("mreports"), new ReportsCommand(this, service, queueGui));
                event.registrar().register("myreports", "View your submitted reports", List.of(), new MyReportsCommand(this, service));
            });
            long window = Math.clamp(getConfig().getLong("submission.duplicate-window-seconds", 300), 0L, 31_536_000L) * 1_000L;
            getServer().getServicesManager().register(MReportsApi.class, new ReportsApiService(repository, window), this, ServicePriority.Normal);
            int days = Math.clamp(getConfig().getInt("storage.retention-days", 90), 1, 36_500);
            repository.purgeClosedBefore(Instant.now().minus(days, ChronoUnit.DAYS)).whenComplete((count, error) -> {
                if (error != null) getLogger().warning("Could not purge expired reports: " + error.getMessage());
                else if (count > 0) getLogger().info("Purged " + count + " expired report(s)");
            });
            int id = Math.max(0, getConfig().getInt("metrics.bstats-id", 0));
            if (getConfig().getBoolean("metrics.enabled", true) && id > 0) new Metrics(this, id);
            new UpdateChecker(this).start();
            getLogger().info("mReports " + getPluginMeta().getVersion() + " is ready");
        } catch (RuntimeException exception) {
            getLogger().severe("mReports failed to start: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (repository != null) repository.close();
    }

    public PluginScheduler scheduler() { return scheduler; }
    public void reloadRuntime() { configValidator.load(); service.reload(); }
}
