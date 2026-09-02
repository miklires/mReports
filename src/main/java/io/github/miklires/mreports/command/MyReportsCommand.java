package io.github.miklires.mreports.command;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.report.ReportService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MyReportsCommand implements BasicCommand {
    private final MReportsPlugin plugin; private final ReportService service;
    public MyReportsCommand(MReportsPlugin plugin, ReportService service) { this.plugin = plugin; this.service = service; }
    @Override public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only"); return; }
        if (!player.hasPermission("mreports.history.own")) { player.sendMessage(ReportService.color(service.text("&cYou do not have permission.", "&cНедостаточно прав."))); return; }
        service.repository().submittedBy(player.getUniqueId(), 10).whenComplete((reports, error) -> plugin.scheduler().player(player, () -> {
            if (error != null) { player.sendMessage(ReportService.color(service.text("&cCould not load your reports.", "&cНе удалось загрузить ваши репорты."))); return; }
            player.sendMessage(ReportService.color(service.text("&eYour recent reports: &f", "&eВаши последние репорты: &f") + reports.size()));
            reports.forEach(report -> player.sendMessage(ReportService.color("&7#" + report.id() + " &f" + report.targetName() + " &e" + report.category() + " &7" + report.status())));
        }));
    }
}
