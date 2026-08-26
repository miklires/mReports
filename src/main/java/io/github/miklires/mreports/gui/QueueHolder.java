package io.github.miklires.mreports.gui;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
public final class QueueHolder implements InventoryHolder {
    private final Map<Integer,Long> reports; private Inventory inventory;
    public QueueHolder(Map<Integer,Long> reports){this.reports=Map.copyOf(reports);} public Long reportAt(int slot){return reports.get(slot);}
    public void inventory(Inventory value){inventory=value;} @Override public @NotNull Inventory getInventory(){return inventory;}
}
