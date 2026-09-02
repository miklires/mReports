package io.github.miklires.mreports.gui;

import io.github.miklires.mreports.MReportsPlugin;
import io.github.miklires.mreports.api.ReportView;
import io.github.miklires.mreports.report.ReportService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class StaffQueueGui implements Listener {
    private final MReportsPlugin plugin; private final ReportService service;
    public StaffQueueGui(MReportsPlugin plugin,ReportService service){this.plugin=plugin;this.service=service;}
    public void open(Player player){service.repository().queue(45).whenComplete((reports,error)->plugin.scheduler().player(player,()->{
        if(error!=null){player.sendMessage(ReportService.color(service.text("&cCould not load the queue.","&cНе удалось загрузить очередь.")));return;}
        Map<Integer,Long> mapping=new LinkedHashMap<>(); int slot=0; for(ReportView report:reports) mapping.put(slot++,report.id());
        QueueHolder holder=new QueueHolder(mapping); Inventory inventory=Bukkit.createInventory(holder,54,ReportService.color(service.text("&cReport Queue","&cОчередь репортов")));holder.inventory(inventory);
        slot=0;for(ReportView report:reports) inventory.setItem(slot++,item(report)); player.openInventory(inventory);
    }));}
    @EventHandler public void click(InventoryClickEvent event){if(!(event.getInventory().getHolder(false) instanceof QueueHolder holder))return;event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player))return;Long id=holder.reportAt(event.getRawSlot());if(id==null)return;
        service.repository().claim(id,player.getUniqueId(),player.getName()).whenComplete((claimed,error)->plugin.scheduler().player(player,()->{
            player.closeInventory(); if(error!=null)player.sendMessage(ReportService.color(service.text("&cStorage error.","&cОшибка хранилища."))); else if(claimed)player.sendMessage(ReportService.color(service.text("&aYou claimed report #","&aВы взяли репорт #")+id+service.text(". Close it with: /reports resolve ",". Закройте: /reports resolve ")+id+" [note]")); else player.sendMessage(ReportService.color(service.text("&eAnother moderator already claimed this report.","&eРепорт уже взят другим модератором.")));
        }));
    }
    private ItemStack item(ReportView report){ItemStack item=new ItemStack(Material.PAPER);ItemMeta meta=item.getItemMeta();meta.setDisplayName(ReportService.color("&e#"+report.id()+" &f"+report.targetName()));meta.setLore(List.of(ReportService.color(service.text("&7From: &f","&7От: &f")+report.reporterName()),ReportService.color(service.text("&7Category: &f","&7Категория: &f")+report.category()),ReportService.color(service.text("&7Status: &f","&7Статус: &f")+report.status()),ReportService.color(service.text("&7Duplicates: &f","&7Повторы: &f")+report.duplicateCount()),ReportService.color("&8"+report.details()),ReportService.color(service.text("&aClick to claim","&aНажмите, чтобы взять"))));item.setItemMeta(meta);return item;}
}
