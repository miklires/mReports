package io.github.miklires.mreports.command;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.api.ReportPriority;
import io.github.miklires.mreports.api.ReportStatus;
import io.github.miklires.mreports.api.ReportView;
import io.github.miklires.mreports.gui.StaffQueueGui;
import io.github.miklires.mreports.report.ReportService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ReportsCommand implements BasicCommand {
    private static final UUID CONSOLE_ID = new UUID(0, 0);
    private final MReportsPlugin plugin;
    private final ReportService service;
    private final StaffQueueGui gui;

    public ReportsCommand(MReportsPlugin plugin, ReportService service, StaffQueueGui gui) {
        this.plugin = plugin; this.service = service; this.gui = gui;
    }

    @Override public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();
        if (!sender.hasPermission("mreports.staff")) { message(sender, service.text("&cYou do not have permission.", "&cНедостаточно прав.")); return; }
        if (args.length == 0) { if (sender instanceof Player player) gui.open(player); else list(sender, service.repository().queue(20)); return; }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("reload")) { reload(sender); return; }
        if (action.equals("search")) { if (args.length < 2) { usage(sender); return; } list(sender, service.repository().search(join(args, 1, 64), 50)); return; }
        if (action.equals("history")) { history(sender, args); return; }
        if (args.length < 2) { usage(sender); return; }
        long id;
        try { id = Long.parseLong(args[1]); } catch (NumberFormatException exception) { usage(sender); return; }
        UUID actor = sender instanceof Player player ? player.getUniqueId() : CONSOLE_ID;
        switch (action) {
            case "view" -> service.repository().find(id).whenComplete((found, error) -> schedule(sender, () -> {
                if (error != null) storageError(sender); else message(sender, found.map(this::line).orElse(service.text("&cReport not found.", "&cРепорт не найден.")));
            }));
            case "evidence" -> service.repository().evidence(id, 30).whenComplete((entries, error) -> schedule(sender, () -> {
                if (error != null) { storageError(sender); return; }
                message(sender, service.text("&eStored chat evidence: &f", "&eСохранённый контекст чата: &f") + entries.size());
                entries.forEach(entry -> message(sender, "&8[" + entry.occurredAt() + "] &7" + entry.sourceName() + ": &f" + entry.body()));
            }));
            case "claim" -> result(sender, service.repository().claim(id, actor, sender.getName()), "Report claimed.", "Репорт взят.", "claimed", id);
            case "release" -> result(sender, service.repository().release(id, actor, sender.getName()), "Report released.", "Репорт освобождён.", "released", id);
            case "priority" -> priority(sender, args, id, actor);
            case "resolve", "reject" -> {
                ReportStatus status = action.equals("resolve") ? ReportStatus.RESOLVED : ReportStatus.REJECTED;
                String note = args.length < 3 ? "" : join(args, 2, 2000);
                result(sender, service.repository().close(id, actor, sender.getName(), status, note), "Report closed: " + status, "Репорт закрыт: " + status, status.name().toLowerCase(Locale.ROOT), id);
            }
            case "note" -> {
                if (args.length < 3) { usage(sender); return; }
                result(sender, service.repository().addNote(id, actor, sender.getName(), join(args, 2, 2000)), "Note added.", "Заметка добавлена.", "note added", id);
            }
            default -> usage(sender);
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("mreports.reload")) { message(sender, service.text("&cYou do not have permission.", "&cНедостаточно прав.")); return; }
        try { plugin.reloadRuntime(); message(sender, service.text("&aConfiguration reloaded.", "&aКонфигурация перезагружена.")); }
        catch (RuntimeException exception) { message(sender, service.text("&cInvalid configuration; see console.", "&cНекорректная конфигурация; проверьте консоль.")); }
    }

    private void history(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender); return; }
        OfflinePlayer player = Bukkit.getPlayerExact(args[1]);
        if (player == null) player = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (player == null) { message(sender, service.text("&cPlayer not found in the server cache.", "&cИгрок не найден в кэше сервера.")); return; }
        list(sender, service.repository().history(player.getUniqueId(), 50));
    }

    private void priority(CommandSender sender, String[] args, long id, UUID actor) {
        if (args.length < 3) { usage(sender); return; }
        try {
            ReportPriority priority = ReportPriority.valueOf(args[2].toUpperCase(Locale.ROOT));
            result(sender, service.repository().setPriority(id, actor, sender.getName(), priority), "Priority updated.", "Приоритет обновлён.", "priority changed", id);
        } catch (IllegalArgumentException exception) { usage(sender); }
    }

    private void list(CommandSender sender, CompletableFuture<List<ReportView>> future) {
        future.whenComplete((reports, error) -> schedule(sender, () -> {
            if (error != null) { storageError(sender); return; }
            message(sender, service.text("&eReports found: &f", "&eНайдено репортов: &f") + reports.size());
            reports.forEach(report -> message(sender, line(report)));
        }));
    }

    private String line(ReportView report) {
        return "&7#" + report.id() + " &f" + report.reporterName() + " -> " + report.targetName() + " &e" + report.category()
                + " &7" + report.status() + " &6" + report.priority() + " &7x" + report.duplicateCount() + "\n&8" + report.details();
    }

    private void result(CommandSender sender, CompletableFuture<Boolean> future, String english, String russian, String action, long id) {
        future.whenComplete((success, error) -> schedule(sender, () -> {
            if (error != null) storageError(sender);
            else { if (success) service.notifyChange(action, id); message(sender, success ? "&a" + service.text(english, russian) : service.text("&cOperation rejected; check the ID, status, and assignee.", "&cОперация отклонена: проверьте ID, статус и обработчика.")); }
        }));
    }

    private void storageError(CommandSender sender) { message(sender, service.text("&cStorage operation failed.", "&cОшибка хранилища.")); }
    private void message(CommandSender sender, String text) { sender.sendMessage(ReportService.color(text)); }
    private void schedule(CommandSender sender, Runnable task) { if (sender instanceof Player player) plugin.scheduler().player(player, task); else plugin.scheduler().global(task); }
    private static String join(String[] args, int start, int maximum) { String value = String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim(); return value.length() <= maximum ? value : value.substring(0, maximum); }
    private static void usage(CommandSender sender) { sender.sendMessage("/reports [view|evidence|claim|release|priority|resolve|reject|note|search|history|reload] ..."); }

    @Override public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
        if (args.length <= 1) return List.of("view", "evidence", "claim", "release", "priority", "resolve", "reject", "note", "search", "history", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("history")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("priority")) return Arrays.stream(ReportPriority.values()).map(value -> value.name().toLowerCase(Locale.ROOT)).toList();
        return List.of();
    }
}
