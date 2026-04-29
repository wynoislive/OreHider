package com.wyno.orehider;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

// Suppress deprecation warnings for ChatColor
@SuppressWarnings("deprecation")
public class OreHiderCommand implements CommandExecutor {

    private final OreHider plugin;

    public OreHiderCommand(OreHider plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // PERMISSION CHECK
        if (!sender.hasPermission("orehider.admin")) {
            sender.sendMessage(ChatColor.RED + "⚠ Permission denied.");
            return true;
        }

        // HELP MENU
        if (args.length == 0) {
            String version = plugin.getDescription().getVersion();
            sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
            sender.sendMessage(ChatColor.GOLD + " OreHider " + ChatColor.GRAY + "v" + version);
            sender.sendMessage(ChatColor.GRAY + " Developed by " + ChatColor.YELLOW + "Wyno");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + " /oh reload " + ChatColor.WHITE + "- Reload configs");
            sender.sendMessage(ChatColor.AQUA + " /oh sync " + ChatColor.WHITE + "- Force sync with Paper");
            sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
            return true;
        }

        // RELOAD COMMAND
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
            
            String msg = plugin.getConfig().getString("discord.messages.update", "Config reloaded.")
                    .replace("{player}", sender.getName());
            plugin.getDiscord().send(msg);
            return true;
        }

        // SYNC COMMAND
        if (args[0].equalsIgnoreCase("sync")) {
            sender.sendMessage(ChatColor.YELLOW + "Checking Paper configurations (Global & Per-World)...");
            boolean changed = plugin.getIntegrator().syncAllConfigurations();
            
            if (changed) {
                sender.sendMessage(ChatColor.GREEN + "✔ Paper configurations updated.");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "⚠ YOU MUST RESTART THE SERVER FOR CHANGES TO APPLY.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "✔ All configurations are already in sync.");
            }
            return true;
        }

        return false;
    }
}