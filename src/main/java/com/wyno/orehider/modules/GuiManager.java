package com.wyno.orehider.modules;

import com.wyno.orehider.OreHider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class GuiManager implements Listener {
    private final OreHider plugin;

    public GuiManager(OreHider plugin) {
        this.plugin = plugin;
    }

    public void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("OreHider Control Panel"));

        // Mode Switcher
        int currentMode = plugin.getConfig().getInt("global.engine-mode");
        inv.setItem(11, createItem(Material.REDSTONE_TORCH, "Engine Mode: " + currentMode, 
                "Click to switch between Mode 1 (Hide) and 2 (Obfuscate)"));

        // Reload
        inv.setItem(13, createItem(Material.EMERALD_BLOCK, "Force Sync & Reload", 
                "Apply changes to Paper config"));

        // Discord Status
        boolean discord = plugin.getConfig().getBoolean("discord.enabled");
        inv.setItem(15, createItem(Material.ENDER_EYE, "Discord Alerts: " + discord, 
                "Click to toggle Discord notifications"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.GOLD));
        meta.lore(Collections.singletonList(Component.text(lore).color(NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().title().equals(Component.text("OreHider Control Panel"))) return;
        e.setCancelled(true);
        
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        Material mat = e.getCurrentItem().getType();
        
        if (mat == Material.REDSTONE_TORCH) {
            int newMode = plugin.getConfig().getInt("global.engine-mode") == 1 ? 2 : 1;
            plugin.getConfig().set("global.engine-mode", newMode);
            plugin.saveConfig();
            p.sendMessage(Component.text("Set Engine Mode to " + newMode));
            openGui(p); // Refresh
        } 
        else if (mat == Material.EMERALD_BLOCK) {
            p.sendMessage(Component.text("Syncing..."));
            plugin.getIntegrator().enforceGlobalConfig();
            p.sendMessage(Component.text("Configs synced! Restart server to apply."));
            p.closeInventory();
        }
        else if (mat == Material.ENDER_EYE) {
            boolean current = plugin.getConfig().getBoolean("discord.enabled");
            plugin.getConfig().set("discord.enabled", !current);
            plugin.saveConfig();
            openGui(p);
        }
    }
}