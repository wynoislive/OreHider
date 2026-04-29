package com.wyno.orehider;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;

public class MiningListener implements Listener {

    private final OreHider plugin;
    // Stores mining history: UUID -> Map<Material, List<Timestamp>>
    private final Map<UUID, Map<Material, List<Long>>> miningHistory = new HashMap<>();

    public MiningListener(OreHider plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("alerts.enabled")) return;

        Player player = event.getPlayer();
        String blockName = event.getBlock().getType().name().toLowerCase();
        List<String> hiddenBlocks = plugin.getConfig().getStringList("global.hidden-blocks");

        // 1. Check if the block is a monitored ore
        if (hiddenBlocks.contains(blockName)) {
            checkSuspiciousActivity(player, event.getBlock().getType(), event);
        }
    }

    // Suppress warning for ChatColor.RED
    @SuppressWarnings("deprecation")
    private void checkSuspiciousActivity(Player player, Material material, BlockBreakEvent event) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int timeWindow = plugin.getConfig().getInt("alerts.time-window", 60) * 1000;
        int threshold = plugin.getConfig().getInt("alerts.threshold", 5);

        // Initialize history for player if not exists
        miningHistory.putIfAbsent(uuid, new HashMap<>());
        Map<Material, List<Long>> playerHistory = miningHistory.get(uuid);
        playerHistory.putIfAbsent(material, new ArrayList<>());

        List<Long> timestamps = playerHistory.get(material);
        
        // 2. Add new mine event
        timestamps.add(now);

        // 3. Remove old events (older than time window)
        timestamps.removeIf(time -> (now - time) > timeWindow);

        // 4. Check Threshold
        if (timestamps.size() >= threshold) {
            triggerAlert(player, material, timestamps.size(), timeWindow / 1000);
            
            // Optional: Prevent mining if "replace blocks" logic implies stopping them
            if (plugin.getConfig().getBoolean("alerts.cancel-event")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "⚠ Mining limit reached. Suspicious activity detected.");
            }
            
            // Clear history to prevent spamming the webhook every single block after threshold
            timestamps.clear();
        }
    }

    private void triggerAlert(Player player, Material material, int count, int seconds) {
        String msg = plugin.getConfig().getString("discord.messages.alert", "🚨 X-Ray Detected: {player}")
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(count))
                .replace("{ore}", material.name())
                .replace("{time}", String.valueOf(seconds));

        plugin.getDiscord().send(msg);
        plugin.getLogger().warning("X-Ray Alert: " + player.getName() + " mined " + count + " " + material.name());
    }
}