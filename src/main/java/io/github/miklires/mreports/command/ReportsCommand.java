package io.github.miklires.mreports.command;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.api.ReportStatus;
import io.github.miklires.mreports.gui.StaffQueueGui;
import io.github.miklires.mreports.report.ReportService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ReportsCommand implements BasicCommand {
    private static final UUID CONSOLE_ID=new UUID(0,0);
    private final MReportsPlugin plugin;private final ReportService service;private final StaffQueueGui gui;
    public ReportsCommand(MReportsPlugin plugin,ReportService service,StaffQueueGui gui){this.plugin=plugin;this.service=service;this.gui=gui;}
    @Override public void execute(@NotNull CommandSourceStack source,@NotNull String[] args){CommandSender sender=source.getSender();if(!sender.hasPermission("mreports.staff")){sender.sendMessage(ReportService.color(service.text("&cYou do not have permission.","&cНедостаточно прав.")));return;}
        if(args.length==0){if(sender instanceof Player player)gui.open(player);else list(sender);return;}
        if(args[0].equalsIgnoreCase("reload")){if(!sender.hasPermission("mreports.reload")){sender.sendMessage(ReportService.color(service.text("&cYou do not have permission.","&cНедостаточно прав.")));return;}try{plugin.reloadRuntime();sender.sendMessage(ReportService.color(service.text("&aConfiguration reloaded.","&aКонфигурация перезагружена.")));}catch(RuntimeException e){sender.sendMessage(ReportService.color(service.text("&cError: ","&cОшибка: ")+e.getMessage()));}return;}
        if(args.length<2){usage(sender);return;}long id;try{id=Long.parseLong(args[1]);}catch(NumberFormatException e){usage(sender);return;}UUID actor=sender instanceof Player p?p.getUniqueId():CONSOLE_ID;
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "claim"->service.repository().claim(id,actor,sender.getName()).thenAccept(ok->reply(sender,ok,service.text("Report claimed.","Репорт взят.")));
            case "resolve","reject"->{ReportStatus status=args[0].equalsIgnoreCase("resolve")?ReportStatus.RESOLVED:ReportStatus.REJECTED;String note=args.length<3?"":String.join(" ",java.util.Arrays.copyOfRange(args,2,args.length));service.repository().close(id,actor,sender.getName(),status,note).thenAccept(ok->reply(sender,ok,service.text("Report closed: ","Репорт закрыт: ")+status));}
            case "note"->{if(args.length<3){usage(sender);return;}String note=String.join(" ",java.util.Arrays.copyOfRange(args,2,args.length));service.repository().addNote(id,actor,sender.getName(),note).thenAccept(ok->reply(sender,ok,service.text("Note added.","Заметка добавлена.")));}
            case "view"->service.repository().find(id).thenAccept(found->plugin.scheduler().global(()->sender.sendMessage(found.map(r->ReportService.color("&e#"+r.id()+" &f"+r.reporterName()+" -> "+r.targetName()+" [&e"+r.category()+"&f] "+r.status()+" x"+r.duplicateCount()+"\n&7"+r.details())).orElse(ReportService.color(service.text("&cReport not found.","&cРепорт не найден."))))));
            default->usage(sender);
        }}
    private void list(CommandSender sender){service.repository().queue(20).thenAccept(rows->plugin.scheduler().global(()->{sender.sendMessage(ReportService.color(service.text("&eOpen reports: ","&eОткрытые репорты: ")+rows.size()));rows.forEach(r->sender.sendMessage(ReportService.color("&7#"+r.id()+" &f"+r.targetName()+" &e"+r.category()+" &7"+r.status())));}));}
    private void reply(CommandSender sender,boolean ok,String success){plugin.scheduler().global(()->sender.sendMessage(ReportService.color(ok?"&a"+success:service.text("&cOperation rejected; check the ID, status, and assignee.","&cОперация отклонена: проверьте ID, статус и обработчика."))));}
    private static void usage(CommandSender sender){sender.sendMessage("/reports [claim|view|resolve|reject|note] <id> [text]");}
    @Override public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source,@NotNull String[] args){if(args.length<=1)return List.of("claim","view","resolve","reject","note","reload");return List.of();}
}
