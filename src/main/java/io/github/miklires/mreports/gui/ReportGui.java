package io.github.miklires.mreports.gui;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.report.ReportService;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ReportGui implements Listener {
    private final MReportsPlugin plugin; private final ReportService service;
    public ReportGui(MReportsPlugin plugin, ReportService service){this.plugin=plugin;this.service=service;}
    public void open(Player player, UUID targetId, String targetName) {
        CategoryHolder holder = new CategoryHolder(targetId, targetName);
        Inventory inventory = Bukkit.createInventory(holder, 27, ReportService.color("&cРепорт: " + targetName)); holder.inventory(inventory);
        int slot=10; for(String category: service.categories()) { if(slot==17) break; inventory.setItem(slot++, item(category)); }
        player.openInventory(inventory);
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof CategoryHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked=event.getCurrentItem(); if(clicked==null || !clicked.hasItemMeta()) return;
        String category=org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName()); player.closeInventory();
        submit(player,holder.targetId(),holder.targetName(),category,"Submitted from category GUI");
    }
    public void submit(Player player, UUID target, String name, String category, String details) {
        Player online=Bukkit.getPlayer(target); boolean exempt=online!=null && online.hasPermission("mreports.exempt");
        service.submit(player,target,name,category,details,exempt).whenComplete((result,error)->plugin.scheduler().player(player,()->{
            if(error!=null){player.sendMessage(ReportService.color("&cНе удалось сохранить репорт."));return;}
            if(!result.success()){player.sendMessage(ReportService.color("&cРепорт отклонён: &f"+result.failure()));return;}
            player.sendMessage(ReportService.color("&aРепорт #"+result.submission().report().id()+" принят"+(result.submission().merged()?" и объединён с предыдущим.":".")));
        }));
    }
    private static ItemStack item(String category){ ItemStack item=new ItemStack(Material.PAPER); ItemMeta meta=item.getItemMeta(); meta.setDisplayName(ReportService.color("&e"+category)); meta.setLore(List.of(ReportService.color("&7Нажмите, чтобы отправить"))); item.setItemMeta(meta); return item; }
}
