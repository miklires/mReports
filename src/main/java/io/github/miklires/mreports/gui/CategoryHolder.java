package io.github.miklires.mreports.gui;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
public final class CategoryHolder implements InventoryHolder {
    private final UUID targetId; private final String targetName; private Inventory inventory;
    public CategoryHolder(UUID targetId, String targetName) { this.targetId=targetId; this.targetName=targetName; }
    public UUID targetId(){return targetId;} public String targetName(){return targetName;} public void inventory(Inventory value){inventory=value;}
    @Override public @NotNull Inventory getInventory(){return inventory;}
}
