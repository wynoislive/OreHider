package com.wyno.orehider.analysis;

import com.wyno.orehider.OreHider;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class MiningListener implements Listener {
    private final OreHider plugin;
    private final boolean hasWorldGuard;

    public MiningListener(OreHider plugin) {
        this.plugin = plugin;
        this.hasWorldGuard = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("orehider.bypass")) return;

        // --- MODULE 3: THE ARCHITECT (Region Checks) ---
        if (hasWorldGuard) {
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(e.getBlock().getLocation());
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            if (!query.testState(loc, null, Flags.BLOCK_BREAK)) return;
        }

        // --- MODULE 1: THE WARDEN (Stats) ---
        Material mat = e.getBlock().getType();
        org.bukkit.World.Environment env = e.getBlock().getWorld().getEnvironment();
        
        // Dimension-Aware Block Classification
        boolean isOre = mat.name().contains("_ORE") || mat == Material.ANCIENT_DEBRIS || 
                        mat == Material.NETHER_QUARTZ_ORE || mat == Material.NETHER_GOLD_ORE ||
                        mat == Material.GILDED_BLACKSTONE;
        boolean isStone = mat == Material.STONE || mat == Material.DEEPSLATE || mat == Material.TUFF || 
                         mat == Material.NETHERRACK || mat == Material.END_STONE || mat == Material.BLACKSTONE;

        if (isOre || isStone) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabase().updateStats(p.getUniqueId(), isOre);
                
                if (isOre) {
                    double ratio = plugin.getDatabase().getRatio(p.getUniqueId());
                    int y = e.getBlock().getLocation().getBlockY();
                    
                    // Dimension-Aware Suspicion Logic
                    boolean suspiciousHeight = false;
                    if (env == org.bukkit.World.Environment.NORMAL && y < 64) suspiciousHeight = true;
                    else if (env == org.bukkit.World.Environment.NETHER && y < 128) suspiciousHeight = true;
                    else if (env == org.bukkit.World.Environment.THE_END) suspiciousHeight = true;

                    if (ratio > 40.0 && suspiciousHeight) {
                       String dimension = env.name().toLowerCase().replace("the_", "");
                       plugin.getLogger().warning("Suspicious mining: " + p.getName() + " in " + dimension + " (Ratio: " + String.format("%.2f", ratio) + "%)");
                       plugin.getDiscord().send(":warning: **Suspicion Alert**: " + p.getName() + " has a mining ratio of **" + String.format("%.2f", ratio) + "%** in the " + dimension + "!");
                    }
                }
            });
        }
    }
}