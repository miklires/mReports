package io.github.miklires.mreports.gui;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
public final class CategoryHolder implements InventoryHolder {
    private final UUID targetId; private final String targetName; private final Map<Integer,String> categories; private Inventory inventory;
    public CategoryHolder(UUID targetId, String targetName, Map<Integer,String> categories) { this.targetId=targetId; this.targetName=targetName; this.categories=Map.copyOf(categories); }
    public UUID targetId(){return targetId;} public String targetName(){return targetName;} public void inventory(Inventory value){inventory=value;}
    public String categoryAt(int slot){return categories.get(slot);}
    @Override public @NotNull Inventory getInventory(){return inventory;}
}
