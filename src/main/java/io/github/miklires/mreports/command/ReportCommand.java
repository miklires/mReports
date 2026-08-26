package io.github.miklires.mreports.command;

import io.github.miklires.mreports.gui.ReportGui;
import io.github.miklires.mreports.report.ReportService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ReportCommand implements BasicCommand {
    private final ReportService service; private final ReportGui gui;
    public ReportCommand(ReportService service, ReportGui gui){this.service=service;this.gui=gui;}
    @Override public void execute(@NotNull CommandSourceStack source,@NotNull String[] args){
        CommandSender sender=source.getSender(); if(!(sender instanceof Player player)){sender.sendMessage("Players only");return;}
        if(!player.hasPermission("mreports.use")){player.sendMessage(ReportService.color("&cНедостаточно прав."));return;}
        if(args.length<1){player.sendMessage("Usage: /report <player> [category] [details]");return;}
        OfflinePlayer target=Bukkit.getPlayerExact(args[0]); if(target==null) target=Bukkit.getOfflinePlayerIfCached(args[0]);
        if(target==null || target.getName()==null){player.sendMessage(ReportService.color("&cИгрок не найден в кэше сервера."));return;}
        if(args.length==1){gui.open(player,target.getUniqueId(),target.getName());return;}
        String details=args.length<3?"Submitted by command":String.join(" ",java.util.Arrays.copyOfRange(args,2,args.length));
        gui.submit(player,target.getUniqueId(),target.getName(),args[1],details);
    }
    @Override public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source,@NotNull String[] args){
        if(args.length<=1){String p=args.length==0?"":args[0].toLowerCase(Locale.ROOT);return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n->n.toLowerCase(Locale.ROOT).startsWith(p)).toList();}
        if(args.length==2){String p=args[1].toUpperCase(Locale.ROOT);return service.categories().stream().filter(c->c.startsWith(p)).toList();} return List.of();
    }
}
