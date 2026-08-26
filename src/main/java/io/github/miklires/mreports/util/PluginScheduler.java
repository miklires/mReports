package io.github.miklires.mreports.util;
import io.github.miklires.mreports.MReportsPlugin;
import org.bukkit.entity.Player;
public final class PluginScheduler {
    private final MReportsPlugin plugin;
    public PluginScheduler(MReportsPlugin plugin) { this.plugin = plugin; }
    public void global(Runnable task) { plugin.getServer().getGlobalRegionScheduler().execute(plugin, task); }
    public void player(Player player, Runnable task) { player.getScheduler().execute(plugin, task, null, 1L); }
}
